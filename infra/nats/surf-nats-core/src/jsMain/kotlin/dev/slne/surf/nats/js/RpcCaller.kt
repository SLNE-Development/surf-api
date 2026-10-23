package dev.slne.surf.nats.js

import dev.slne.surf.nats.descriptor.NatsResultKind
import dev.slne.surf.nats.js.external.nats.core.Msg
import dev.slne.surf.nats.js.external.nats.core.NatsPublishOptions
import dev.slne.surf.nats.js.external.nats.core.RequestOptions
import dev.slne.surf.nats.js.external.nats.core.SubscriptionOptions
import dev.slne.surf.nats.js.external.nats.core.createInbox
import dev.slne.surf.nats.js.external.nats.jetstream.JetStreamPublishOptions
import dev.slne.surf.nats.js.external.nats.jetstream.StreamExpectations
import dev.slne.surf.nats.js.external.protobuf.DescMethod
import dev.slne.surf.nats.js.external.protobuf.toBinary
import dev.slne.surf.nats.protocol.PayloadBucket
import dev.slne.surf.nats.protocol.ProtocolDefaults
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.protocol.ServiceNames
import dev.slne.surf.nats.rpc.Recipients
import dev.slne.surf.nats.runtime.RuntimeDefaults
import dev.slne.surf.nats.runtime.rpc.CallFlow
import dev.slne.surf.nats.runtime.rpc.PersistentRules
import js.objects.unsafeJso
import js.typedarrays.Uint8Array
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.await
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import node.crypto.randomUUID
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration

/**
 * Client side of the surf-nats RPC protocol, following the JVM `RpcCallerImpl` flow by flow.
 */
internal class RpcCaller(private val runtime: NatsRuntime) {

    private val connection get() = runtime.connection
    private val payloads get() = runtime.payloads

    // Responders offload large reply bodies to the transient bucket; the caller deletes them after reading.
    private val replyBody: suspend (Msg) -> Uint8Array<*> = {
        payloads.resolve(it, PayloadBucket.TRANSIENT, delete = true)
    }

    // Every method except ALL_INSTANCES methods with a result; undefined for UNIT methods.
    suspend fun call(methodDesc: DescMethod, owner: String?, request: ProtoMessageInit?, timeoutMs: Int?): ProtoMessage? {
        val method = Contracts.method(methodDesc)
        require(method.resultKind != NatsResultKind.ANSWER) {
            "${method.label} is an ALL_INSTANCES method with a result and returns an Answer: use callAnswer"
        }
        val call = prepare(method, owner, request, timeoutMs)

        when (method.flow) {
            CallFlow.CORE -> Unit
            CallFlow.FIRE -> {
                fire(call)
                return undefined
            }

            CallFlow.PERSISTENT_RPC -> return persistentRpc(call)
        }

        val headers = runtime.headers()
        val sent = coreBody(call, headers)
        return when (method.recipients) {
            Recipients.ANY_INSTANCE -> callOne(call, headers, sent)
            Recipients.ALL_INSTANCES -> {
                fanOut(call, headers, sent)
                undefined
            }
        }
    }

    // ALL_INSTANCES methods with a result.
    suspend fun callAnswer(methodDesc: DescMethod, owner: String?, request: ProtoMessageInit?, timeoutMs: Int?): Answer {
        val method = Contracts.method(methodDesc)
        require(method.resultKind == NatsResultKind.ANSWER) {
            "${method.label} is not an ALL_INSTANCES method with a result: use call"
        }
        val call = prepare(method, owner, request, timeoutMs)

        val headers = runtime.headers()
        val sent = coreBody(call, headers)
        return collectAnswers(call, headers, sent)
    }

    private fun prepare(method: MethodContract, owner: String?, request: ProtoMessageInit?, timeoutMs: Int?): Call {
        val timeout = positiveTimeoutOrNull(timeoutMs, "Call timeout") ?: method.defaultTimeout
        val names = method.service.names(owner)
        val subject = method.subject(owner)
        val body = toBinary(method.input, MessageRules.prepare(method.input, request))
        return Call(method, names, subject, timeout, body)
    }

    private class Call(
        val method: MethodContract,
        val names: ServiceNames,
        val subject: String,
        val timeout: Duration,
        val body: Uint8Array<*>,
    ) {
        val timeoutMs: Int get() = timeout.inWholeMilliseconds.toInt()
    }

    private suspend fun coreBody(call: Call, headers: OutgoingHeaders): Uint8Array<*> {
        val flow = call.method.flow
        return payloads.offload(headers, call.body, call.timeout, flow.bucket, stampDeadline = flow.sendsDeadline)
    }

    private suspend fun callOne(call: Call, headers: OutgoingHeaders, body: Uint8Array<*>): ProtoMessage? {
        val options = unsafeJso<RequestOptions> {
            timeout = call.timeoutMs
            this.headers = headers.msgHdrs
        }

        val reply = try {
            connection.request(call.subject, body, options).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw mapRequestError(e, call.subject, call.timeoutMs)
        }
        return call.method.decoder.decode(reply, replyBody)
    }

    // ALL_INSTANCES without a result: publish to all instances without a reply inbox.
    private fun fanOut(call: Call, headers: OutgoingHeaders, body: Uint8Array<*>) {
        connection.publish(call.subject, body, unsafeJso<NatsPublishOptions> { this.headers = headers.msgHdrs })
    }

    private suspend fun collectAnswers(call: Call, headers: OutgoingHeaders, body: Uint8Array<*>): Answer {
        val inbox = createInbox()
        val replies = Channel<Msg>(Channel.UNLIMITED)
        val subscription = connection.subscribe(inbox, unsafeJso<SubscriptionOptions> {
            callback = { error, message ->
                if (error == null) replies.trySend(message) else replies.close(error)
            }
        })

        try {
            connection.publish(call.subject, body, unsafeJso<NatsPublishOptions> {
                reply = inbox
                this.headers = headers.msgHdrs
            })

            val answers = AllAnswers(call.method.decoder, replyBody, call.subject)
            return withTimeoutOrNull(call.timeout) { answers.collect(replies) } ?: answers.noAnswer()
        } finally {
            subscription.unsubscribe()
            replies.close()
        }
    }

    private suspend fun fire(call: Call) {
        PersistentRules.requireFireTimeout(call.timeout, RuntimeDefaults.dedupeWindow) { call.method.label }

        val flow = call.method.flow
        val stream = call.names.stream
        val headers = runtime.headers()
        headers.set(ProtocolHeaders.NATS_MSG_ID, randomUUID())
        val sent = payloads.offload(
            headers,
            call.body,
            flow.offloadLifetime(call.timeout),
            flow.bucket,
            Wire.headerLineBytes(ProtocolHeaders.NATS_EXPECTED_STREAM, stream),
            stampDeadline = flow.sendsDeadline,
        )

        try {
            withTimeout(call.timeout) {
                publishPersistent(call.subject, headers, sent, stream)
            }
        } catch (_: TimeoutCancellationException) {
            currentCoroutineContext().ensureActive()
            throw NatsCallTimeoutError(call.subject, call.timeoutMs)
        }
    }

    private suspend fun persistentRpc(call: Call): ProtoMessage? {
        val flow = call.method.flow
        val stream = call.names.stream
        val inbox = createInbox()
        val headers = runtime.headers()
        headers.set(ProtocolHeaders.REPLY_TO, inbox)
        headers.set(ProtocolHeaders.NATS_MSG_ID, randomUUID())
        headers.set(ProtocolHeaders.NATS_TTL, "${ProtocolDefaults.natsTtlSeconds(call.timeout)}s")
        val sent = payloads.offload(
            headers,
            call.body,
            flow.offloadLifetime(call.timeout),
            flow.bucket,
            Wire.headerLineBytes(ProtocolHeaders.NATS_EXPECTED_STREAM, stream),
            stampDeadline = flow.sendsDeadline,
        )

        val reply = CompletableDeferred<Msg>()
        val subscription = connection.subscribe(inbox, unsafeJso<SubscriptionOptions> {
            max = 1
            callback = { error, message ->
                if (error == null) reply.complete(message) else reply.completeExceptionally(error)
            }
        })

        try {
            val publishPhase = PersistentRules.publishPhase(call.timeout, RuntimeDefaults.dedupeWindow)
            val response = withTimeoutOrNull(call.timeout) {
                withTimeoutOrNull(publishPhase) {
                    publishPersistent(call.subject, headers, sent, stream)
                } ?: return@withTimeoutOrNull null
                reply.await()
            } ?: throw NatsCallTimeoutError(call.subject, call.timeoutMs)
            return call.method.decoder.decode(response, replyBody)
        } finally {
            subscription.unsubscribe()
        }
    }

    /**
     * Publishes until a PubAck arrives, repeating timed-out attempts with the same `Nats-Msg-Id` so the stream
     * deduplicates them; a rejection becomes [NatsRpcError].
     */
    private suspend fun publishPersistent(subject: String, headers: OutgoingHeaders, body: Uint8Array<*>, stream: String) {
        val options = unsafeJso<JetStreamPublishOptions> {
            this.headers = headers.msgHdrs
            expect = unsafeJso<StreamExpectations> { streamName = stream }
            JsRuntimeDefaults.jetStreamPublishTimeout?.let { timeout = it.inWholeMilliseconds.toInt() }
        }

        while (true) {
            try {
                runtime.jetStream.publish(subject, body, options).await()
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                if (!isTimeout(e)) {
                    throw NatsRpcError("JetStream publish on $subject failed: ${e.message}", e)
                }
                currentCoroutineContext().ensureActive()
            }
        }
    }
}
