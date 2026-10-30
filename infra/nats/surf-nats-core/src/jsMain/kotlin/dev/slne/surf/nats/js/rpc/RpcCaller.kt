package dev.slne.surf.nats.js.rpc

import dev.slne.surf.nats.descriptor.NatsResultKind
import dev.slne.surf.nats.js.api.Answer
import dev.slne.surf.nats.js.api.ProtoMessage
import dev.slne.surf.nats.js.api.ProtoMessageInit
import dev.slne.surf.nats.js.client.JsRuntimeDefaults
import dev.slne.surf.nats.js.client.NatsClientImpl
import dev.slne.surf.nats.js.codec.decodeToString
import dev.slne.surf.nats.js.codec.header
import dev.slne.surf.nats.js.codec.statusCode
import dev.slne.surf.nats.js.error.NatsCallTimeoutError
import dev.slne.surf.nats.js.error.NatsRemoteError
import dev.slne.surf.nats.js.error.NatsRpcError
import dev.slne.surf.nats.js.external.nats.core.Msg
import dev.slne.surf.nats.js.external.nats.core.MsgHdrs
import dev.slne.surf.nats.js.external.nats.core.NatsPublishOptions
import dev.slne.surf.nats.js.external.nats.core.RequestOptions
import dev.slne.surf.nats.js.external.nats.core.SubscriptionOptions
import dev.slne.surf.nats.js.external.nats.core.createInbox
import dev.slne.surf.nats.js.external.nats.jetstream.JetStreamPublishOptions
import dev.slne.surf.nats.js.external.nats.jetstream.StreamExpectations
import dev.slne.surf.nats.js.interop.positiveTimeoutOrNull
import dev.slne.surf.nats.protocol.PayloadBucket
import dev.slne.surf.nats.protocol.ProtocolDefaults
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.rpc.Recipients
import dev.slne.surf.nats.runtime.RuntimeDefaults
import dev.slne.surf.nats.runtime.headerLineBytes
import dev.slne.surf.nats.runtime.randomId
import dev.slne.surf.nats.runtime.rpc.AllReply
import dev.slne.surf.nats.runtime.rpc.CallFlow
import dev.slne.surf.nats.runtime.rpc.FailureRecorder
import dev.slne.surf.nats.runtime.rpc.PersistentRules
import dev.slne.surf.nats.runtime.rpc.classifyAllReply
import js.typedarrays.Uint8Array
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration

/**
 * Client side of the surf-nats RPC protocol, following the JVM `RpcCallerImpl` flow by flow.
 */
internal class RpcCaller(private val client: NatsClientImpl) {
    private val connection get() = client.connection
    private val payloads get() = client.payloads

    /**
     * Returns `undefined` for UNIT methods, the decoded response for VALUE methods and an [Answer] for ANSWER methods.
     */
    suspend fun call(method: MethodDescriptor, owner: String?, request: ProtoMessageInit?, timeoutMs: Int?): Any? {
        val timeout = positiveTimeoutOrNull(timeoutMs, "Call timeout") ?: method.timeout
        val subject = method.subject(owner)

        val flow = method.flow
        if (flow != CallFlow.CORE) return callPersistent(method, subject, method.stream(owner), flow, request, timeout)

        val headers = client.headers()
        val encoded = method.encodeRequest(request)
        val body = payloads.offload(headers, encoded, timeout, flow.bucket, stampDeadline = flow.sendsDeadline)

        return when (method.recipients) {
            Recipients.ANY_INSTANCE -> callOne(method, subject, headers, body, timeout)
            Recipients.ALL_INSTANCES -> callAll(method, subject, headers, body, timeout)
        }
    }

    private suspend fun callOne(
        method: MethodDescriptor,
        subject: String,
        headers: MsgHdrs,
        body: Uint8Array<*>,
        timeout: Duration,
    ): Any? {
        val timeoutMs = timeout.inWholeMilliseconds.toInt()
        val reply = try {
            connection.request(subject, body, RequestOptions(timeout = timeoutMs, headers = headers)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw mapRequestError(e, subject, timeoutMs)
        }

        return decodeReply(method, reply)
    }

    private suspend fun callAll(
        method: MethodDescriptor,
        subject: String,
        headers: MsgHdrs,
        body: Uint8Array<*>,
        timeout: Duration,
    ): Any? {
        if (method.result == NatsResultKind.UNIT) {
            // Fan-out to all instances
            connection.publish(subject, body, NatsPublishOptions(headers = headers))
            return undefined
        }

        // Without presence tracking the call ends with the first known answer, no responders or the timeout
        val inbox = createInbox()
        val replies = Channel<Msg>(Channel.UNLIMITED)
        val subscription = connection.subscribe(
            inbox,
            SubscriptionOptions(
                callback = { error, reply ->
                    if (error == null) replies.trySend(reply) else replies.close(error)
                },
            ),
        )

        try {
            connection.publish(subject, body, NatsPublishOptions(reply = inbox, headers = headers))

            val call = AllCall()

            return withTimeoutOrNull(timeout) {
                collect(method, subject, replies, call)
            } ?: call.noAnswer()
        } finally {
            subscription.unsubscribe()
            replies.close()
        }
    }

    private suspend fun collect(
        method: MethodDescriptor,
        subject: String,
        replies: ReceiveChannel<Msg>,
        call: AllCall,
    ): Answer {
        for (reply in replies) {
            val kind = classifyAllReply(
                reply.statusCode(),
                reply.header(ProtocolHeaders.ERROR),
                reply.header(ProtocolHeaders.ANSWER),
            )
            when (kind) {
                AllReply.NO_RESPONDERS -> return call.noAnswer()
                AllReply.OTHER_STATUS, AllReply.UNKNOWN -> continue
                AllReply.REMOTE_ERROR -> {
                    val origin = reply.header(ProtocolHeaders.ORIGIN)
                    val remoteType = reply.header(ProtocolHeaders.ERROR)!!
                    call.record(NatsRemoteError(remoteType, reply.data.decodeToString()))
                    client.logger.warn("ALL_INSTANCES responder $origin failed on $subject: $remoteType")
                }

                AllReply.ANSWER -> {
                    val origin = reply.header(ProtocolHeaders.ORIGIN)
                    val decoded = try {
                        decodeBody(method, reply)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        call.record(e)
                        client.logger.warn("Undecodable ALL_INSTANCES answer from $origin on $subject", e)
                        null
                    }
                    if (decoded != null) return Answer(known = true, value = decoded)
                }
            }
        }
        error("reply channel closed")
    }

    private suspend fun decodeReply(method: MethodDescriptor, reply: Msg): Any? {
        reply.header(ProtocolHeaders.ERROR)?.let { remoteType ->
            throw NatsRemoteError(remoteType, reply.data.decodeToString())
        }

        return when (method.result) {
            NatsResultKind.UNIT -> undefined
            NatsResultKind.VALUE -> decodeBody(method, reply)
            NatsResultKind.ANSWER -> error("ANSWER replies of ${method.label} are collected by callAll")
        }
    }

    private suspend fun decodeBody(method: MethodDescriptor, reply: Msg): ProtoMessage {
        return method.decodeResponse(
            payloads.resolve(reply, PayloadBucket.TRANSIENT, delete = true),
            reply.header(ProtocolHeaders.CONTENT_TYPE),
            "reply on ${reply.subject}",
        )
    }

    // FIRE and PERSISTENT_RPC
    private suspend fun callPersistent(
        method: MethodDescriptor,
        subject: String,
        stream: String,
        flow: CallFlow,
        request: ProtoMessageInit?,
        timeout: Duration,
    ): Any? {
        if (flow == CallFlow.FIRE) {
            PersistentRules.requireFireTimeout(timeout, RuntimeDefaults.dedupeWindow, method.label)
        }

        val headers = client.headers()
        val encoded = method.encodeRequest(request)

        val inbox = if (flow.sendsReplyTo) createInbox() else null
        if (inbox != null) headers.set(ProtocolHeaders.REPLY_TO, inbox)
        headers.set(ProtocolHeaders.NATS_MSG_ID, randomId())
        if (flow.sendsTtl) headers.set(ProtocolHeaders.NATS_TTL, "${ProtocolDefaults.natsTtlSeconds(timeout)}s")
        val body = payloads.offload(
            headers,
            encoded,
            flow.offloadLifetime(timeout),
            flow.bucket,
            // nats.js adds the expected stream header for the publish options after the offload check
            extraHeaderBytes = headerLineBytes(ProtocolHeaders.NATS_EXPECTED_STREAM, stream),
            stampDeadline = flow.sendsDeadline,
        )
        val options = publishOptions(headers, stream)

        if (inbox == null) {
            try {
                withTimeout(timeout) {
                    publishPersistent(subject, body, options)
                }
            } catch (_: TimeoutCancellationException) {
                currentCoroutineContext().ensureActive()
                throw NatsCallTimeoutError(subject, timeout.inWholeMilliseconds.toInt())
            }
            return undefined
        }

        val reply = CompletableDeferred<Msg>()
        val subscription = connection.subscribe(
            inbox,
            SubscriptionOptions(
                max = 1,
                callback = { error, received ->
                    if (error == null) reply.complete(received) else reply.completeExceptionally(error)
                },
            ),
        )

        try {
            val response = withTimeoutOrNull(timeout) {
                withTimeoutOrNull(PersistentRules.publishPhase(timeout, RuntimeDefaults.dedupeWindow)) {
                    publishPersistent(subject, body, options)
                } ?: return@withTimeoutOrNull null
                reply.await()
            } ?: throw NatsCallTimeoutError(subject, timeout.inWholeMilliseconds.toInt())
            return decodeReply(method, response)
        } finally {
            subscription.unsubscribe()
        }
    }

    private fun publishOptions(headers: MsgHdrs, stream: String): JetStreamPublishOptions {
        return JetStreamPublishOptions(
            headers = headers,
            expect = StreamExpectations(streamName = stream),
            timeout = JsRuntimeDefaults.jetStreamPublishTimeout?.inWholeMilliseconds?.toInt(),
        )
    }

    /**
     * Publishes until a PubAck arrives, repeating timed-out attempts with the same `Nats-Msg-Id` so the stream
     * deduplicates them; a rejection becomes [NatsRpcError].
     */
    private suspend fun publishPersistent(subject: String, body: Uint8Array<*>, options: JetStreamPublishOptions) {
        while (true) {
            try {
                client.jetStream.publish(subject, body, options).await()
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                if (!isTimeout(e)) throw NatsRpcError("JetStream publish on $subject failed: ${e.message}", e)
                currentCoroutineContext().ensureActive()
            }
        }
    }

    private class AllCall : FailureRecorder() {
        fun noAnswer(): Answer {
            throwIfAny()
            return Answer(known = false)
        }
    }
}
