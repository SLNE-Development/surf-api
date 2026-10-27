package dev.slne.surf.nats.js.rpc

import dev.slne.surf.nats.descriptor.NatsResultKind
import dev.slne.surf.nats.js.api.Answer
import dev.slne.surf.nats.js.api.ProtoMessage
import dev.slne.surf.nats.js.api.ProtoMessageInit
import dev.slne.surf.nats.js.client.JsRuntimeDefaults
import dev.slne.surf.nats.js.client.NatsRuntime
import dev.slne.surf.nats.js.codec.NatsHeaders
import dev.slne.surf.nats.js.codec.OutgoingMessage
import dev.slne.surf.nats.js.contract.Contracts
import dev.slne.surf.nats.js.error.NatsCallTimeoutError
import dev.slne.surf.nats.js.error.NatsRpcError
import dev.slne.surf.nats.js.external.nats.core.Msg
import dev.slne.surf.nats.js.external.nats.core.RequestOptions
import dev.slne.surf.nats.js.external.nats.core.SubscriptionOptions
import dev.slne.surf.nats.js.external.nats.core.createInbox
import dev.slne.surf.nats.js.external.nats.jetstream.JetStreamPublishOptions
import dev.slne.surf.nats.js.external.nats.jetstream.StreamExpectations
import dev.slne.surf.nats.js.external.protobuf.DescMethod
import dev.slne.surf.nats.protocol.PayloadBucket
import dev.slne.surf.nats.protocol.ProtocolDefaults
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.rpc.Recipients
import dev.slne.surf.nats.runtime.RuntimeDefaults
import dev.slne.surf.nats.runtime.rpc.CallFlow
import dev.slne.surf.nats.runtime.rpc.PersistentRules
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import node.crypto.randomUUID
import kotlin.coroutines.cancellation.CancellationException

/**
 * Client side of the surf-nats RPC protocol, following the JVM `RpcCallerImpl` flow by flow.
 */
internal class RpcCaller(private val runtime: NatsRuntime) {
    private val connection get() = runtime.connection
    private val payloads get() = runtime.payloads

    private val replyBody: ReplyBody = { msg ->
        payloads.resolve(msg, PayloadBucket.TRANSIENT, delete = true)
    }

    /**
     * Calls every method except `ALL_INSTANCES` methods with a result; returns `undefined` for UNIT methods.
     */
    suspend fun call(
        method: DescMethod,
        request: ProtoMessageInit?,
        owner: String?,
        timeoutMs: Int?,
    ): ProtoMessage? {
        val contract = Contracts.method(method)
        require(contract.resultKind != NatsResultKind.ANSWER) {
            "${contract.label} is an ALL_INSTANCES method with a result and returns an Answer: use callAnswer"
        }

        val call = Call.prepare(contract, request, owner, timeoutMs)

        return when (call.flow) {
            CallFlow.CORE -> when (contract.recipients) {
                Recipients.ANY_INSTANCE -> requestOne(call)
                Recipients.ALL_INSTANCES -> {
                    publishToAll(call)
                    undefined
                }
            }

            CallFlow.FIRE -> {
                fire(call)
                undefined
            }

            CallFlow.PERSISTENT_RPC -> persistentRequest(call)
        }
    }

    /**
     * Calls an `ALL_INSTANCES` method with a result.
     */
    suspend fun callAnswer(
        method: DescMethod,
        request: ProtoMessageInit?,
        owner: String?,
        timeoutMs: Int?
    ): Answer {
        val contract = Contracts.method(method)
        require(contract.resultKind == NatsResultKind.ANSWER) {
            "${contract.label} is not an ALL_INSTANCES method with a result: use call"
        }

        val call = Call.prepare(contract, request, owner, timeoutMs)

        return requestAll(call)
    }

    private suspend fun requestOne(call: Call): ProtoMessage? {
        val message = coreMessage(call)
        val options = RequestOptions(timeout = call.timeoutMs, headers = message.headers.msgHdrs)

        val reply = try {
            connection.request(call.subject, message.body, options).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw mapRequestError(e, call.subject, call.timeoutMs)
        }

        return decoder(call).decode(reply)
    }

    private suspend fun publishToAll(call: Call) {
        val message = coreMessage(call)
        connection.publish(call.subject, message.body, message.publishOptions())
    }

    private suspend fun requestAll(call: Call): Answer {
        val message = coreMessage(call)
        val inbox = createInbox()
        val replies = Channel<Msg>(Channel.UNLIMITED)
        val subscription = connection.subscribe(
            subject = inbox,
            opts = SubscriptionOptions(
                callback = { error, reply ->
                    if (error == null) {
                        replies.trySend(reply)
                    } else {
                        replies.close(error)
                    }
                }
            )
        )

        try {
            connection.publish(call.subject, message.body, message.publishOptions(replyTo = inbox))

            val answers = AllAnswers(decoder(call), call.subject)
            return withTimeoutOrNull(call.timeout) { // TODO: presence tracking
                answers.collect(replies)
            } ?: answers.noAnswer()
        } finally {
            subscription.unsubscribe()
            replies.close()
        }
    }

    private suspend fun fire(call: Call) {
        PersistentRules.requireFireTimeout(
            call.timeout,
            RuntimeDefaults.dedupeWindow
        ) { call.method.label }
        val message = persistentMessage(call, replyTo = null)

        try {
            withTimeout(call.timeout) { publishPersistent(call, message) }
        } catch (_: TimeoutCancellationException) {
            currentCoroutineContext().ensureActive()
            throw NatsCallTimeoutError(call.subject, call.timeoutMs)
        }
    }

    private suspend fun persistentRequest(call: Call): ProtoMessage? {
        val inbox = createInbox()
        val message = persistentMessage(call, replyTo = inbox)

        val reply = CompletableDeferred<Msg>()
        val subscription = connection.subscribe(
            subject = inbox,
            opts = SubscriptionOptions(
                max = 1,
                callback = { error, received ->
                    if (error == null) {
                        reply.complete(received)
                    } else {
                        reply.completeExceptionally(error)
                    }
                }
            )
        )

        try {
            val publishPhase = PersistentRules.publishPhase(
                call.timeout,
                RuntimeDefaults.dedupeWindow
            )
            val response = withTimeoutOrNull(call.timeout) {
                withTimeoutOrNull(publishPhase) {
                    publishPersistent(call, message)
                } ?: return@withTimeoutOrNull null
                reply.await()
            } ?: throw NatsCallTimeoutError(call.subject, call.timeoutMs)

            return decoder(call).decode(response)
        } finally {
            subscription.unsubscribe()
        }
    }

    private suspend fun coreMessage(call: Call): OutgoingMessage {
        val headers = runtime.headers()
        val body = payloads.offload(
            headers,
            call.body,
            call.timeout,
            call.flow.bucket,
            stampDeadline = call.flow.sendsDeadline,
        )
        return OutgoingMessage(headers, body)
    }

    private suspend fun persistentMessage(call: Call, replyTo: String?): OutgoingMessage {
        val flow = call.flow
        val headers = runtime.headers()
        if (replyTo != null) headers.set(ProtocolHeaders.REPLY_TO, replyTo)
        headers.set(ProtocolHeaders.NATS_MSG_ID, randomUUID())
        if (flow.sendsTtl) {
            headers.set(
                ProtocolHeaders.NATS_TTL,
                "${ProtocolDefaults.natsTtlSeconds(call.timeout)}s"
            )
        }

        val body = payloads.offload(
            headers,
            call.body,
            flow.offloadLifetime(call.timeout),
            flow.bucket,
            extraHeaderBytes = NatsHeaders.lineSize(
                ProtocolHeaders.NATS_EXPECTED_STREAM,
                call.stream
            ),
            stampDeadline = flow.sendsDeadline,
        )
        return OutgoingMessage(headers, body)
    }

    /**
     * Publishes until a PubAck arrives, repeating timed-out attempts with the same `Nats-Msg-Id` so the stream
     * deduplicates them; a rejection becomes [NatsRpcError].
     */
    private suspend fun publishPersistent(call: Call, message: OutgoingMessage) {
        val options = JetStreamPublishOptions(
            headers = message.headers.msgHdrs,
            expect = StreamExpectations(streamName = call.stream),
            timeout = JsRuntimeDefaults.jetStreamPublishTimeout?.inWholeMilliseconds?.toInt(),
        )

        while (true) {
            try {
                runtime.jetStream.publish(call.subject, message.body, options).await()
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                if (!isTimeout(e)) throw NatsRpcError(
                    "JetStream publish on ${call.subject} failed: ${e.message}",
                    e
                )
                currentCoroutineContext().ensureActive()
            }
        }
    }

    private fun decoder(call: Call): ReplyDecoder {
        return ReplyDecoder(call.method.output, call.method.resultKind, replyBody)
    }
}
