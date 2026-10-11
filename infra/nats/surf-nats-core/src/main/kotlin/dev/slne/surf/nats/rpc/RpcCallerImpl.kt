package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.*
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.codec.NatsHeaders
import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.internal.NatsTimeoutElement
import dev.slne.surf.nats.jetstream.JetStreamProvisioner
import dev.slne.surf.nats.rpc.payload.PayloadBucket
import dev.slne.surf.nats.rpc.payload.PayloadStore
import io.nats.client.JetStreamStatusException
import io.nats.client.Message
import io.nats.client.PublishOptions
import io.nats.client.impl.Headers
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.future.await
import java.util.*
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.toJavaDuration

@InternalNatsApi
internal class RpcCallerImpl(
    private val client: NatsClientImpl,
    private val service: ServiceDescriptor,
) : RpcCaller {

    private val presence = service.presenceSubject?.let { subject ->
        PresenceTracker(client, subject).also { tracker ->
            tracker.start(client.replyDispatcher())
        }
    }

    override suspend fun call(index: Int, args: Array<Any?>): Any? {
        val method = service.methods[index]
        val timeout = currentCoroutineContext()[NatsTimeoutElement]?.timeout ?: method.timeout

        if (method.persistent) {
            require(timeout <= RpcDefaults.DUPLICATE_WINDOW) {
                "Timeout $timeout for persistent ${service.type.name}.${method.method.name} exceeds the JetStream duplicate window of ${RpcDefaults.DUPLICATE_WINDOW}"
            }
            return callPersistent(method, args, timeout)
        }

        val headers = NatsHeaders.base(client.name, method.format)
            .add(
                NatsHeaders.DEADLINE,
                (System.currentTimeMillis() + timeout.inWholeMilliseconds).toString()
            )
        val encoded = client.codec.encode(method.arguments, args, method.format)
        val body = PayloadStore.offload(headers, encoded, timeout, PayloadBucket.TRANSIENT)

        return when (method.responders) {
            ResponderMode.ONE -> callOne(method, headers, body, timeout)
            ResponderMode.ALL -> callAll(method, headers, body, timeout)
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
        } catch (_: TimeoutCancellationException) {
            currentCoroutineContext().ensureActive()
            throw NatsRequestTimeoutException(method.subject, timeout)
        } catch (_: CancellationException) {
            currentCoroutineContext().ensureActive()
            throw NatsRequestTimeoutException(method.subject, timeout)
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
            // Fan-out to all responders
            connection.publish(method.subject, headers, body)
            return Unit
        }

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
                    return Answer.None
                }
                continue
            }

            val headers = if (reply.hasHeaders()) reply.headers else null
            val origin = headers?.getFirst(NatsHeaders.ORIGIN)

            if (origin != null) {
                call.answered.add(origin)
                presence?.seen(origin)
            }

            val remoteType = headers?.getFirst(NatsHeaders.ERROR)
            when {
                remoteType != null -> {
                    call.fail(NatsRemoteException(remoteType, reply.data.decodeToString()))
                    client.logger.warn("ALL responder $origin failed on ${method.subject}: $remoteType")
                }

                headers?.getFirst(NatsHeaders.ANSWER) == NatsHeaders.ANSWER_NONE -> Unit
                else -> return Answer.Of(
                    client.codec.decode(
                        result.serializer,
                        PayloadStore.resolve(reply, PayloadBucket.TRANSIENT, delete = true),
                        headers?.getFirst(NatsHeaders.CONTENT_TYPE)
                    )
                )
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
        val headers = if (reply.hasHeaders()) reply.headers else null

        headers?.getFirst(NatsHeaders.ERROR)?.let { remoteType ->
            throw NatsRemoteException(remoteType, reply.data.decodeToString())
        }

        val contentType = headers?.getFirst(NatsHeaders.CONTENT_TYPE)

        return when (val result = method.result) {
            ResultKind.Unit -> Unit
            is ResultKind.Value -> client.codec.decode(
                result.serializer,
                PayloadStore.resolve(reply, PayloadBucket.TRANSIENT, delete = true),
                contentType
            )

            is ResultKind.Answer -> {
                if (headers?.getFirst(NatsHeaders.ANSWER) == NatsHeaders.ANSWER_NONE) {
                    Answer.None
                } else {
                    Answer.Of(
                        client.codec.decode(
                            result.serializer,
                            PayloadStore.resolve(reply, PayloadBucket.TRANSIENT, delete = true),
                            contentType
                        )
                    )
                }
            }
        }
    }


    private suspend fun callPersistent(
        method: MethodDescriptor,
        args: Array<Any?>,
        timeout: Duration,
    ): Any? {
        val headers = NatsHeaders.base(client.name, method.format)
        val encoded = client.codec.encode(method.arguments, args, method.format)

        if (method.result == ResultKind.Unit) {
            val body = PayloadStore.offload(
                headers,
                encoded,
                RpcDefaults.PERSIST_MAX_AGE,
                PayloadBucket.PERSISTENT,
            )
            try {
                withTimeout(timeout) {
                    publishPersistent(method, headers, body, ttl = null)
                }
            } catch (_: TimeoutCancellationException) {
                currentCoroutineContext().ensureActive()
                throw NatsRequestTimeoutException(method.subject, timeout)
            }
            return Unit
        }

        val inbox = NatsConnectionManager.connection().createInbox()
        headers.add(
            NatsHeaders.DEADLINE,
            (System.currentTimeMillis() + timeout.inWholeMilliseconds).toString()
        )
        headers.add(NatsHeaders.REPLY_TO, inbox)
        val body = PayloadStore.offload(headers, encoded, timeout, PayloadBucket.TRANSIENT)

        val reply = CompletableDeferred<Message>()
        val dispatcher = client.replyDispatcher()
        val subscription = dispatcher.subscribe(inbox) { reply.complete(it) }

        try {
            val completed = withTimeoutOrNull(timeout) {
                publishPersistent(method, headers, body, ttl = timeout)
                Completed(decodeReply(method, reply.await()))
            } ?: throw NatsRequestTimeoutException(method.subject, timeout)
            return completed.value
        } finally {
            runCatching { dispatcher.unsubscribe(subscription) }
        }
    }

    private suspend fun publishPersistent(
        method: MethodDescriptor,
        headers: Headers,
        body: ByteArray,
        ttl: Duration?,
    ) {
        val streams = checkNotNull(service.streams)
        JetStreamProvisioner.ensureStreams(streams)

        val options = PublishOptions.builder()
            .messageId(UUID.randomUUID().toString())
            .expectedStream(streams.stream)
            .apply {
                if (ttl != null) {
                    messageTtlSeconds(ttl.inWholeSeconds.coerceAtLeast(1).toInt())
                }
            }
            .build()

        while (true) {
            try {
                NatsConnectionManager.connection()
                    .jetStream()
                    .publishAsync(method.subject, headers, body, options)
                    .await()
                return
            } catch (e: CancellationException) {
                currentCoroutineContext().ensureActive()
                client.logger.debug(
                    "No PubAck for ${method.subject}, publishing again with the same Nats-Msg-Id",
                    e
                )
            }
        }
    }

    private class Completed(@JvmField val value: Any?)

    private class AllCall {
        var failure: NatsRemoteException? = null
        val answered = ObjectOpenHashSet<String>()

        fun fail(exception: NatsRemoteException) {
            if (failure == null) {
                failure = exception
            } else {
                failure?.addSuppressed(exception)
            }
        }

        fun noAnswer(): Answer.None {
            failure?.let { throw it }
            return Answer.None
        }
    }
}