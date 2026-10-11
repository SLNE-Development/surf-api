package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.*
import dev.slne.surf.nats.bytecode.ServiceInvoker
import dev.slne.surf.nats.bytecode.ServiceInvokerFactory
import dev.slne.surf.nats.bytecode.call
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.codec.NatsHeaders
import dev.slne.surf.nats.codec.deadlineMillis
import dev.slne.surf.nats.codec.header
import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.jetstream.JetStreamProvisioner
import dev.slne.surf.nats.rpc.payload.PayloadBucket
import dev.slne.surf.nats.rpc.payload.PayloadStore
import dev.slne.surf.nats.util.rethrowIfFatal
import io.nats.client.Dispatcher
import io.nats.client.Message
import io.nats.client.MessageConsumer
import io.nats.client.MessageHandler
import io.nats.client.PublishOptions
import io.nats.client.impl.Headers
import io.nats.client.support.NatsJetStreamClientError
import io.nats.client.support.NatsJetStreamConstants
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@InternalNatsApi
internal class ServiceRegistration(
    private val client: NatsClientImpl,
    val descriptor: ServiceDescriptor,
    implementation: Any,
) : NatsRegistration {

    private val invokers = descriptor.methods.map {
        ServiceInvokerFactory.create(implementation, it.method)
    }

    private val announcer = descriptor.presenceSubject?.let { subject ->
        PresenceAnnouncer(client, subject)
    }

    private val persistentBySubject = descriptor.methods
        .filter { it.persistent }
        .associateBy { it.subject }

    private val coroutineNames = descriptor.methods.map { method ->
        val kind = if (method.persistent) "persist" else "rpc"
        CoroutineName("surf-nats/${client.name}/$kind/${method.subject}")
    }

    private val scope = client.clientScope + SupervisorJob(client.clientScope.coroutineContext.job)

    private val active = AtomicBoolean(true)
    override val isClosed get() = !active.get()

    @Volatile
    private var dispatcher: Dispatcher? = null

    @Volatile
    private var messageConsumer: MessageConsumer? = null

    suspend fun subscribe() {
        val connection = NatsConnectionManager.connection()
        val dispatcher = connection.createDispatcher()
        this.dispatcher = dispatcher

        for (method in descriptor.methods) {
            if (method.persistent) continue
            val invoker = invokers[method.index]
            val handler = MessageHandler { onRequest(method, invoker, it) }
            when (method.responders) {
                ResponderMode.ONE -> dispatcher.subscribe(method.subject, descriptor.responderGroup, handler)
                ResponderMode.ALL -> dispatcher.subscribe(method.subject, handler)
            }
        }

        descriptor.streams?.let { streams ->
            JetStreamProvisioner.ensureStreams(streams)
            withContext(Dispatchers.IO) {
                val consumer = connection.getStreamContext(streams.stream)
                    .createOrUpdateConsumer(JetStreamProvisioner.consumerConfig(descriptor.responderGroup))
                messageConsumer = consumer.consume(dispatcher) { onPersistent(it) }
            }
        }

        announcer?.start(dispatcher)

        withContext(Dispatchers.IO) {
            connection.flush(5.seconds.toJavaDuration())
        }
    }

    private fun onRequest(
        methodDescriptor: MethodDescriptor,
        invoker: ServiceInvoker,
        message: Message,
    ) {
        val contentType = message.header(NatsHeaders.CONTENT_TYPE)
        val format = contentType?.let(WireFormat::fromContentType) ?: WireFormat.CBOR
        val deadline = message.deadlineMillis()

        val left = remaining(deadline, methodDescriptor.timeout)
        if (!left.isPositive()) return dropExpired(message, left)

        val replyTo = message.replyTo
            ?: return onFanOut(methodDescriptor, invoker, message, contentType, deadline)

        scope.launch(coroutineNames[methodDescriptor.index]) {
            val timeout = remaining(deadline, methodDescriptor.timeout)
            if (!timeout.isPositive()) return@launch dropExpired(message, timeout)

            val reply = try {
                val completed = withTimeoutOrNull(timeout) {
                    val data = PayloadStore.resolve(
                        message,
                        PayloadBucket.TRANSIENT,
                        delete = methodDescriptor.responders == ResponderMode.ONE
                    )
                    val args = client.codec.decode(methodDescriptor.arguments, data, contentType)
                    Completed(invoker.call(args))
                }
                if (completed == null) {
                    // The requester has already given up
                    client.logger.warn("Service method ${methodDescriptor.subject} did not complete within $timeout and was cancelled")
                    return@launch
                }
                encodeResult(methodDescriptor, completed.value, format)
            } catch (e: CancellationException) {
                ensureActive()
                errorReply(methodDescriptor, e, format)
            } catch (e: Throwable) {
                e.rethrowIfFatal()
                errorReply(methodDescriptor, e, format)
            }

            send(methodDescriptor, replyTo, reply, format, timeout)
        }
    }

    private fun onFanOut(
        methodDescriptor: MethodDescriptor,
        invoker: ServiceInvoker,
        message: Message,
        contentType: String?,
        deadline: Long?,
    ) {
        scope.launch(coroutineNames[methodDescriptor.index]) {
            val left = remaining(deadline, methodDescriptor.timeout)
            if (!left.isPositive()) return@launch dropExpired(message, left)

            try {
                val data = PayloadStore.resolve(
                    message,
                    PayloadBucket.TRANSIENT,
                    delete = methodDescriptor.responders == ResponderMode.ONE
                )
                invoker.call(client.codec.decode(methodDescriptor.arguments, data, contentType))
            } catch (e: CancellationException) {
                ensureActive()
                logFanOutFailure(methodDescriptor, e)
            } catch (e: Throwable) {
                e.rethrowIfFatal()
                logFanOutFailure(methodDescriptor, e)
            }
        }
    }

    private fun logFanOutFailure(method: MethodDescriptor, e: Throwable) {
        client.logger.error("Fan-out of ${descriptor.type.name}.${method.method.name} failed", e)
    }

    private fun onPersistent(message: Message) {
        val method = persistentBySubject[message.subject]
        if (method == null) {
            scope.launch(CoroutineName("surf-nats/${client.name}/persist/unknown")) {
                val error = IllegalStateException("No persistent method is registered for ${message.subject}")
                failFire(message, error, permanent = false)
            }
            return
        }
        val meta = message.metaData()
        val context = NatsPersistentMessageContext(
            messageId = message.header(NatsJetStreamConstants.MSG_ID_HDR)
                ?: meta.streamSequence().toString(),
            deliveryCount = meta.deliveredCount(),
        )

        // Ignore already timed out messages
        val deadline = message.deadlineMillis()
        if (deadline != null && deadline <= System.currentTimeMillis()) return message.term()

        scope.launch(context + coroutineNames[method.index]) {
            val keepAlive = launch(Dispatchers.IO) {
                while (isActive) {
                    delay(RpcDefaults.ACK_WAIT / 2)
                    try {
                        message.inProgress()
                    } catch (e: Exception) {
                        client.logger.debug("Failed to extend the ack wait of ${message.subject}", e)
                    }
                }
            }
            try {
                if (method.result == ResultKind.Unit) {
                    handleFire(method, message)
                } else {
                    handlePersistentRpc(method, message, deadline)
                }
            } finally {
                keepAlive.cancel()
            }
        }
    }

    private suspend fun handleFire(method: MethodDescriptor, message: Message) {
        val data = try {
            PayloadStore.resolve(message, PayloadBucket.PERSISTENT, delete = false)
        } catch (e: CancellationException) {
            currentCoroutineContext().ensureActive()
            return failFire(message, e, permanent = false)
        } catch (e: Throwable) {
            e.rethrowIfFatal()
            return failFire(message, e, permanent = isObjectNotFound(e))
        }

        val arguments = try {
            client.codec.decode(method.arguments, data, message.header(NatsHeaders.CONTENT_TYPE))
        } catch (e: CancellationException) {
            currentCoroutineContext().ensureActive()
            return failFire(message, e, permanent = false)
        } catch (e: Throwable) {
            e.rethrowIfFatal()
            return failFire(message, e, permanent = true)
        }

        val completed: Unit? = try {
            withTimeoutOrNull(RpcDefaults.FIRE_MAX_DURATION) {
                invokers[method.index].call(arguments)
                Unit
            }
        } catch (e: CancellationException) {
            currentCoroutineContext().ensureActive()
            return failFire(message, e, permanent = false)
        } catch (e: Throwable) {
            e.rethrowIfFatal()
            return failFire(message, e, permanent = false)
        }

        if (completed == null) {
            val error = TimeoutException("Handler did not complete within ${RpcDefaults.FIRE_MAX_DURATION}")
            return failFire(message, error, permanent = false)
        }

        if (!PayloadStore.isOffloaded(message)) {
            message.ack()
            return
        }

        try {
            runInterruptible(Dispatchers.IO) {
                message.ackSync(RpcDefaults.ACK_SYNC_TIMEOUT.toJavaDuration())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            client.logger.warn("Ack of ${message.subject} was not confirmed, keeping its payload", e)
            return
        }

        try {
            PayloadStore.delete(message, PayloadBucket.PERSISTENT)
        } catch (e: Exception) {
            client.logger.warn("Failed to delete payload of ${message.subject}", e)
        }
    }

    private fun isObjectNotFound(e: Throwable): Boolean {
        return e is IllegalArgumentException &&
                e.message?.startsWith("[${NatsJetStreamClientError.OsObjectNotFound.id()}]") == true
    }

    private suspend fun failFire(message: Message, e: Throwable, permanent: Boolean) {
        val delivered = message.metaData().deliveredCount()
        client.logger.error(
            "Persistent call on ${message.subject} failed (delivery $delivered${if (permanent) ", permanent" else ""})",
            e
        )
        if ((permanent || delivered >= RpcDefaults.MAX_DELIVER) && deadLetter(message, e)) {
            message.term()
        } else {
            val backoff = RpcDefaults.NAK_BACKOFF[(delivered - 1).toInt()
                .coerceIn(0, RpcDefaults.NAK_BACKOFF.lastIndex)]
            message.nakWithDelay(backoff.toJavaDuration())
        }
    }

    private suspend fun deadLetter(message: Message, e: Throwable): Boolean {
        val streams = checkNotNull(descriptor.streams)
        val headers = Headers(message.headers)
            .put(NatsHeaders.ERROR, e.javaClass.name)
            .put(NatsHeaders.DEAD_SUBJECT, message.subject)

        headers.remove(headers.keySet().filter { it.startsWith("Nats-", ignoreCase = true) })

        val options = PublishOptions.builder()
            .messageId("${streams.dlqStream}-${message.metaData().streamSequence()}")
            .build()

        return try {
            NatsConnectionManager.connection()
                .jetStream()
                .publishAsync(streams.dlqSubject, headers, message.data, options)
                .await()
            true
        } catch (dlq: CancellationException) {
            currentCoroutineContext().ensureActive()
            client.logger.error("Failed to dead-letter ${message.subject}: no PubAck", dlq)
            false
        } catch (dlq: Exception) {
            client.logger.error("Failed to dead-letter ${message.subject}", dlq)
            false
        }
    }

    private suspend fun handlePersistentRpc(
        method: MethodDescriptor,
        message: Message,
        deadline: Long?,
    ) {
        val replyTo = message.header(NatsHeaders.REPLY_TO)
        val contentType = message.header(NatsHeaders.CONTENT_TYPE)
        val format = contentType?.let(WireFormat::fromContentType) ?: WireFormat.CBOR
        val timeout = remaining(deadline, method.timeout)

        val reply = try {
            val completed = withTimeoutOrNull(timeout) {
                val data = PayloadStore.resolve(message, PayloadBucket.TRANSIENT, delete = false)
                Completed(
                    invokers[method.index].call(
                        client.codec.decode(
                            method.arguments,
                            data,
                            contentType,
                        )
                    )
                )
            }
            if (completed == null) {
                message.term()
                return // Caller has already given up
            }
            encodeResult(method, completed.value, format)
        } catch (e: CancellationException) {
            currentCoroutineContext().ensureActive()
            errorReply(method, e, format)
        } catch (e: Throwable) {
            e.rethrowIfFatal()
            errorReply(method, e, format)
        }

        if (replyTo != null) {
            send(method, replyTo, reply, format, timeout)
        }
        message.ack()

        try {
            PayloadStore.delete(message, PayloadBucket.TRANSIENT)
        } catch (e: Exception) {
            client.logger.warn("Failed to delete payload of ${message.subject}", e)
        }
    }

    override fun close() {
        if (!active.compareAndSet(true, false)) return
        announcer?.close()
        messageConsumer?.let {
            try {
                it.close()
            } catch (e: Exception) {
                client.logger.warn("Failed to close message consumer", e)
            }
        }
        dispatcher?.let {
            try {
                NatsConnectionManager.connection().closeDispatcher(it)
            } catch (e: Exception) {
                client.logger.warn("Failed to close service dispatcher", e)
            }
        }
        scope.cancel("Service registration ${descriptor.type.name} closed")
        client.unregisterService(this)
        client.unregisterRegistration(this)
    }

    fun discard() {
        active.set(false)
        scope.cancel("Service registration ${descriptor.type.name} discarded")
    }

    private fun remaining(deadline: Long?, fallback: Duration): Duration {
        return deadline?.let { (it - System.currentTimeMillis()).milliseconds } ?: fallback
    }

    private fun dropExpired(message: Message, remaining: Duration) {
        client.logger.warn(
            "Dropping request on ${message.subject} that expired ${-remaining.inWholeMilliseconds} ms ago " +
                    "(check the clock synchronization of the hosts if this happens regularly)"
        )
    }

    private class Completed(@JvmField val value: Any?)
    private class Reply(@JvmField val headers: Headers, @JvmField val body: ByteArray)

    private fun encodeResult(
        method: MethodDescriptor,
        value: Any?,
        format: WireFormat,
    ): Reply {
        val headers = NatsHeaders.base(client.name, format)
        return when (val result = method.result) {
            ResultKind.Unit -> Reply(headers, NatsHeaders.EMPTY_BODY)
            is ResultKind.Value -> Reply(
                headers,
                client.codec.encode(result.serializer, value, format)
            )

            is ResultKind.Answer -> when (val answer = value as Answer<*>) {
                is Answer.Of -> Reply(
                    headers,
                    client.codec.encode(result.serializer, answer.value, format)
                )

                Answer.None -> Reply(
                    headers.add(NatsHeaders.ANSWER, NatsHeaders.ANSWER_NONE),
                    NatsHeaders.EMPTY_BODY
                )
            }
        }
    }

    private fun errorReply(
        method: MethodDescriptor,
        throwable: Throwable,
        format: WireFormat,
    ): Reply {
        client.logger.error(
            "Service method ${descriptor.type.name}.${method.method.name} failed",
            throwable
        )

        val headers = NatsHeaders.base(client.name, format)
            .add(NatsHeaders.ERROR, throwable.javaClass.name)

        return Reply(
            headers,
            throwable.message.orEmpty().take(RpcDefaults.MAX_ERROR_MESSAGE).encodeToByteArray()
        )
    }

    private suspend fun send(
        method: MethodDescriptor,
        replyTo: String,
        reply: Reply,
        format: WireFormat,
        timeout: Duration,
    ) {
        val connection = NatsConnectionManager.connection()
        try {
            val body =
                PayloadStore.offload(reply.headers, reply.body, timeout, PayloadBucket.TRANSIENT)
            connection.publish(replyTo, reply.headers, body)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val fallback = errorReply(method, e, format)
            connection.publish(replyTo, fallback.headers, fallback.body)
        }
    }
}