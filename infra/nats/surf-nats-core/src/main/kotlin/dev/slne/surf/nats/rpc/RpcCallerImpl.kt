package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.codec.NatsHeaders
import dev.slne.surf.nats.codec.WireSerializers
import dev.slne.surf.nats.codec.header
import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.internal.NatsTimeoutElement
import dev.slne.surf.nats.jetstream.JetStreamProvisioner
import dev.slne.surf.nats.rpc.payload.PayloadBucket
import dev.slne.surf.nats.rpc.payload.PayloadStore
import io.nats.client.JetStreamApiException
import io.nats.client.JetStreamStatusException
import io.nats.client.Message
import io.nats.client.PublishOptions
import io.nats.client.impl.Headers
import io.nats.client.support.NatsJetStreamConstants
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

        if (method.persistent) {
            if (method.result == ResultKind.Unit) {
                require(timeout <= RpcDefaults.DUPLICATE_WINDOW) {
                    "Timeout $timeout for persistent ${service.type.name}.${method.method.name} without a result exceeds the JetStream duplicate window of ${RpcDefaults.DUPLICATE_WINDOW}"
                }
            }
            return callPersistent(method, args, timeout)
        }

        val headers = client.headers(method.format)
            .add(
                NatsHeaders.DEADLINE,
                NatsHeaders.deadline(timeout)
            ) // Put the deadline in now, so the header size is correct for the object store
        val encoded = client.codec.encode(method.arguments, args, method.format)
        val body = PayloadStore.offload(headers, encoded, timeout, PayloadBucket.TRANSIENT)
        headers.put(
            NatsHeaders.DEADLINE,
            NatsHeaders.deadline(timeout)
        ) // Reset the deadline after a possible object store upload

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

        if (method.result == ResultKind.Unit) {
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
        val result = method.result as ResultKind.Answer

        for (reply in replies) {
            if (reply.isStatusMessage) {
                if (reply.status.isNoResponders) {
                    return Answer.Unknown
                }
                continue
            }

            val origin = reply.header(NatsHeaders.ORIGIN)

            if (origin != null) {
                call.answered.add(origin)
                presence?.seen(origin)
            }

            val remoteType = reply.header(NatsHeaders.ERROR)
            when {
                remoteType != null -> {
                    call.fail(NatsRemoteException(remoteType, reply.data.decodeToString()))
                    client.logger.warn("ALL_INSTANCES responder $origin failed on ${method.subject}: $remoteType")
                }

                reply.header(NatsHeaders.ANSWER) == NatsHeaders.ANSWER_UNKNOWN -> Unit
                else -> {
                    val decoded = try {
                        Completed(decodeBody(result.serializer, reply))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        call.fail(e)
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

    private suspend fun decodeReply(
        method: MethodDescriptor,
        reply: Message
    ): Any? {
        reply.header(NatsHeaders.ERROR)?.let { remoteType ->
            throw NatsRemoteException(remoteType, reply.data.decodeToString())
        }

        return when (val result = method.result) {
            ResultKind.Unit -> Unit
            is ResultKind.Value -> decodeBody(result.serializer, reply)
            is ResultKind.Answer -> {
                if (reply.header(NatsHeaders.ANSWER) == NatsHeaders.ANSWER_UNKNOWN) {
                    Answer.Unknown
                } else {
                    Answer.Known(decodeBody(result.serializer, reply))
                }
            }
        }
    }

    private suspend fun decodeBody(serializers: WireSerializers<Any?>, reply: Message): Any? {
        return client.codec.decode(
            serializers,
            PayloadStore.resolve(reply, PayloadBucket.TRANSIENT, delete = true),
            reply.header(NatsHeaders.CONTENT_TYPE)
        )
    }

    private suspend fun callPersistent(
        method: MethodDescriptor,
        args: Array<Any?>,
        timeout: Duration,
    ): Any? {
        val streams = checkNotNull(service.streams)
        val headers = client.headers(method.format)
        val encoded = client.codec.encode(method.arguments, args, method.format)

        if (method.result == ResultKind.Unit) {
            val options = publishOptions(streams.stream, ttl = null)
            val body = PayloadStore.offload(
                headers,
                encoded,
                RpcDefaults.PERSIST_MAX_AGE,
                PayloadBucket.PERSISTENT,
                options.headerBytes(),
            )
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

        val inbox = NatsConnectionManager.connection().createInbox()
        headers.add(NatsHeaders.DEADLINE, NatsHeaders.deadline(timeout))
        headers.add(NatsHeaders.REPLY_TO, inbox)
        val options = publishOptions(streams.stream, ttl = minOf(timeout, RpcDefaults.PERSIST_MAX_AGE))
        val body = PayloadStore.offload(
            headers,
            encoded,
            timeout,
            PayloadBucket.TRANSIENT,
            options.headerBytes()
        )
        headers.put(NatsHeaders.DEADLINE, NatsHeaders.deadline(timeout))

        val reply = CompletableDeferred<Message>()
        val dispatcher = client.replyDispatcher()
        val subscription = dispatcher.subscribe(inbox) { reply.complete(it) }

        try {
            val response = withTimeoutOrNull(timeout) {
                withTimeoutOrNull(minOf(timeout, RpcDefaults.DUPLICATE_WINDOW)) {
                    publishPersistent(method, headers, body, options)
                } ?: return@withTimeoutOrNull null
                reply.await()
            } ?: throw NatsCallTimeoutException(method.subject, timeout)
            return decodeReply(method, response)
        } finally {
            runCatching { dispatcher.unsubscribe(subscription) }
        }
    }

    private fun publishOptions(stream: String, ttl: Duration?): PublishOptions {
        return PublishOptions.builder()
            .messageId(UUID.randomUUID().toString())
            .expectedStream(stream)
            .apply {
                if (ttl != null) {
                    messageTtlSeconds(
                        ((ttl.inWholeMilliseconds + 999) / 1000).coerceAtLeast(1).toInt()
                    )
                }
            }
            .build()
    }

    private fun PublishOptions.headerBytes(): Int {
        fun line(key: String, value: String?): Int {
            return if (value.isNullOrEmpty()) 0 else key.length + value.length + 3
        }

        return line(NatsJetStreamConstants.EXPECTED_STREAM_HDR, expectedStream) +
                line(NatsJetStreamConstants.MSG_ID_HDR, messageId) +
                line(NatsJetStreamConstants.MSG_TTL_HDR, messageTtl)
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

    private class AllCall {
        var failure: Exception? = null
        val answered = ObjectOpenHashSet<String>()

        fun fail(exception: Exception) {
            if (failure == null) {
                failure = exception
            } else {
                failure?.addSuppressed(exception)
            }
        }

        fun noAnswer(): Answer.Unknown {
            failure?.let { throw it }
            return Answer.Unknown
        }
    }
}
