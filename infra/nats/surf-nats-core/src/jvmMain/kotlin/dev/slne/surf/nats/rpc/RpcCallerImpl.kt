package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.codec.NatsHeaders
import dev.slne.surf.nats.codec.header
import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.descriptor.NatsResultKind
import dev.slne.surf.nats.internal.NatsTimeoutElement
import dev.slne.surf.nats.jetstream.JetStreamProvisioner
import dev.slne.surf.nats.protocol.PayloadBucket
import dev.slne.surf.nats.protocol.ProtocolDefaults
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.rpc.payload.PayloadStore
import dev.slne.surf.nats.runtime.RuntimeDefaults
import dev.slne.surf.nats.runtime.rpc.AllReply
import dev.slne.surf.nats.runtime.rpc.CallFlow
import dev.slne.surf.nats.runtime.rpc.FailureRecorder
import dev.slne.surf.nats.runtime.rpc.PersistentRules
import dev.slne.surf.nats.runtime.rpc.classifyAllReply
import io.nats.client.JetStreamApiException
import io.nats.client.JetStreamStatusException
import io.nats.client.Message
import io.nats.client.PublishOptions
import io.nats.client.impl.Headers
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.future.await
import java.io.IOException
import java.util.*
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.toJavaDuration

@InternalNatsApi
internal class RpcCallerImpl(
    private val client: NatsClientImpl,
    private val service: ServiceDescriptor,
) : RpcCaller {

    private val presence = service.presenceSubject?.let { subject -> PresenceTracker(client, subject) }

    fun start() {
        presence?.start(client.replyDispatcher())
    }

    override suspend fun call(index: Int, args: Array<Any?>): Any? {
        client.checkOpen()
        val method = service.methods[index]
        val timeout = currentCoroutineContext()[NatsTimeoutElement]?.timeout ?: method.timeout

        val flow = method.flow
        if (flow != CallFlow.CORE) return callPersistent(method, flow, args, timeout)

        val headers = client.headers()
        val encoded = method.encodeRequest(args)
        val body = PayloadStore.offload(headers, encoded, timeout, flow.bucket, stampDeadline = flow.sendsDeadline)

        return when (method.recipients) {
            Recipients.ANY_INSTANCE -> callOne(method, headers, body, timeout)
            Recipients.ALL_INSTANCES -> callAll(method, headers, body, timeout)
        }
    }

    private suspend fun callOne(
        method: MethodDescriptor,
        headers: Headers,
        body: ByteArray,
        timeout: Duration
    ): Any? {
        val reply = try {
            withTimeout(timeout) {
                NatsConnectionManager.connection()
                    .requestWithTimeout(method.subject, headers, body, timeout.toJavaDuration())
                    .await()
            }
        } catch (_: JetStreamStatusException) {
            throw NatsNoRespondersException(method.subject)
        } catch (_: CancellationException) {
            currentCoroutineContext().ensureActive()
            throw NatsCallTimeoutException(method.subject, timeout)
        }

        return decodeReply(method, reply)
    }

    private suspend fun callAll(
        method: MethodDescriptor,
        headers: Headers,
        body: ByteArray,
        timeout: Duration
    ): Any {
        val connection = NatsConnectionManager.connection()

        if (method.result == NatsResultKind.UNIT) {
            // Fan-out to all instances
            connection.publish(method.subject, headers, body)
            return Unit
        }

        presence?.refreshAfterReconnect()

        val replyDispatcher = client.replyDispatcher()
        val inbox = connection.createInbox()
        val replies = Channel<Message>(Channel.UNLIMITED)
        val subscription = replyDispatcher.subscribe(inbox, replies::trySend)

        try {
            connection.publish(method.subject, inbox, headers, body)

            val call = AllCall()

            return withTimeoutOrNull(timeout) {
                collect(method, replies, call)
            } ?: call.noAnswer()
        } finally {
            runCatching { replyDispatcher.unsubscribe(subscription) }
            replies.close()
        }
    }

    private suspend fun collect(
        method: MethodDescriptor,
        replies: ReceiveChannel<Message>,
        call: AllCall
    ): Answer<*> {
        for (reply in replies) {
            val status = if (reply.isStatusMessage) reply.status.code else 0
            val kind = classifyAllReply(status, reply.header(ProtocolHeaders.ERROR), reply.header(ProtocolHeaders.ANSWER))
            when (kind) {
                AllReply.NO_RESPONDERS -> return call.noAnswer()
                AllReply.OTHER_STATUS -> continue
                AllReply.UNKNOWN -> answered(call, reply)
                AllReply.REMOTE_ERROR -> {
                    val origin = answered(call, reply)
                    val remoteType = reply.header(ProtocolHeaders.ERROR)!!
                    call.record(NatsRemoteException(remoteType, reply.data.decodeToString()))
                    client.logger.warn("ALL_INSTANCES responder $origin failed on ${method.subject}: $remoteType")
                }

                AllReply.ANSWER -> {
                    val origin = answered(call, reply)
                    val decoded = try {
                        Completed(decodeBody(method, reply))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        call.record(e)
                        client.logger.warn(
                            "Undecodable ALL_INSTANCES answer from $origin on ${method.subject}",
                            e
                        )
                        null
                    }
                    if (decoded != null) return Answer.Known(decoded.value)
                }
            }

            if (presence?.allAnswered(call.answered) == true) {
                return call.noAnswer()
            }
        }
        error("reply channel closed")
    }

    // Records the responder of a reply for the presence-based early end; returns its origin.
    private fun answered(call: AllCall, reply: Message): String? {
        val origin = reply.header(ProtocolHeaders.ORIGIN)
        if (origin != null) {
            call.answered.add(origin)
            presence?.seen(origin)
        }
        return origin
    }

    private suspend fun decodeReply(
        method: MethodDescriptor,
        reply: Message
    ): Any? {
        reply.header(ProtocolHeaders.ERROR)?.let { remoteType ->
            throw NatsRemoteException(remoteType, reply.data.decodeToString())
        }

        return when (method.result) {
            NatsResultKind.UNIT -> Unit
            NatsResultKind.VALUE -> decodeBody(method, reply)
            NatsResultKind.ANSWER -> error("ANSWER replies of ${method.label} are collected by callAll")
        }
    }

    private suspend fun decodeBody(method: MethodDescriptor, reply: Message): Any {
        return method.decodeResponse(
            PayloadStore.resolve(reply, PayloadBucket.TRANSIENT, delete = true),
            reply.header(ProtocolHeaders.CONTENT_TYPE)
        )
    }

    // FIRE and PERSISTENT_RPC
    private suspend fun callPersistent(
        method: MethodDescriptor,
        flow: CallFlow,
        args: Array<Any?>,
        timeout: Duration,
    ): Any? {
        if (flow == CallFlow.FIRE) {
            PersistentRules.requireFireTimeout(timeout, RuntimeDefaults.dedupeWindow, method.label)
        }

        val streams = checkNotNull(service.streams)
        val headers = client.headers()
        val encoded = method.encodeRequest(args)

        val inbox = if (flow.sendsReplyTo) NatsConnectionManager.connection().createInbox() else null
        if (inbox != null) headers.add(ProtocolHeaders.REPLY_TO, inbox)
        val ttlSeconds = if (flow.sendsTtl) ProtocolDefaults.natsTtlSeconds(timeout) else null
        val options = publishOptions(streams.stream, ttlSeconds)
        val body = PayloadStore.offload(
            headers,
            encoded,
            flow.offloadLifetime(timeout),
            flow.bucket,
            options.headerBytes(),
            stampDeadline = flow.sendsDeadline,
        )

        if (inbox == null) {
            try {
                withTimeout(timeout) {
                    publishPersistent(method, headers, body, options)
                }
            } catch (_: TimeoutCancellationException) {
                currentCoroutineContext().ensureActive()
                throw NatsCallTimeoutException(method.subject, timeout)
            }
            return Unit
        }

        val reply = CompletableDeferred<Message>()
        val dispatcher = client.replyDispatcher()
        val subscription = dispatcher.subscribe(inbox) { reply.complete(it) }

        try {
            val response = withTimeoutOrNull(timeout) {
                withTimeoutOrNull(PersistentRules.publishPhase(timeout, RuntimeDefaults.dedupeWindow)) {
                    publishPersistent(method, headers, body, options)
                } ?: return@withTimeoutOrNull null
                reply.await()
            } ?: throw NatsCallTimeoutException(method.subject, timeout)
            return decodeReply(method, response)
        } finally {
            runCatching { dispatcher.unsubscribe(subscription) }
        }
    }

    private fun publishOptions(stream: String, ttlSeconds: Int?): PublishOptions {
        return PublishOptions.builder()
            .messageId(UUID.randomUUID().toString())
            .expectedStream(stream)
            .apply { if (ttlSeconds != null) messageTtlSeconds(ttlSeconds) }
            .build()
    }

    // Headers jnats adds for the publish options after the offload check
    private fun PublishOptions.headerBytes(): Int {
        return NatsHeaders.lineBytes(ProtocolHeaders.NATS_EXPECTED_STREAM, expectedStream) +
                NatsHeaders.lineBytes(ProtocolHeaders.NATS_MSG_ID, messageId) +
                NatsHeaders.lineBytes(ProtocolHeaders.NATS_TTL, messageTtl)
    }

    private suspend fun publishPersistent(
        method: MethodDescriptor,
        headers: Headers,
        body: ByteArray,
        options: PublishOptions,
    ) {
        JetStreamProvisioner.ensureStreams(checkNotNull(service.streams))

        val jetStream = NatsConnectionManager.connection().jetStream()
        while (true) {
            try {
                jetStream.publishAsync(method.subject, headers, body, options).await()
                return
            } catch (e: CancellationException) {
                currentCoroutineContext().ensureActive()
                client.logger.debug(
                    "No PubAck for ${method.subject}, publishing again with the same Nats-Msg-Id",
                    e
                )
            } catch (e: RuntimeException) {
                val rejection = generateSequence(e.cause) { it.cause }
                    .take(3)
                    .firstOrNull { it is IOException || it is JetStreamApiException }
                    ?: throw e
                throw NatsRpcException("JetStream publish on ${method.subject} failed: ${rejection.message}", rejection)
            }
        }
    }

    private class Completed(@JvmField val value: Any?)

    private class AllCall : FailureRecorder() {
        val answered = ObjectOpenHashSet<String>()

        fun noAnswer(): Answer.Unknown {
            throwIfAny()
            return Answer.Unknown
        }
    }
}
