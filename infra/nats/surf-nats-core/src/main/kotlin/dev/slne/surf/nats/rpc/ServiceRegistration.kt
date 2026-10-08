package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.*
import dev.slne.surf.nats.bytecode.ServiceInvoker
import dev.slne.surf.nats.bytecode.ServiceInvokerFactory
import dev.slne.surf.nats.bytecode.call
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.codec.NatsHeaders
import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.jetstream.JetStreamProvisioner
import dev.slne.surf.nats.rpc.payload.PayloadBucket
import dev.slne.surf.nats.rpc.payload.PayloadStore
import io.nats.client.Dispatcher
import io.nats.client.Message
import io.nats.client.MessageConsumer
import io.nats.client.MessageHandler
import io.nats.client.impl.Headers
import io.nats.client.support.NatsJetStreamConstants
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import org.jetbrains.annotations.Blocking
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

    private val scope = client.clientScope + SupervisorJob(client.clientScope.coroutineContext.job)

    private val active = AtomicBoolean(true)
    override val isActive get() = active.get()

    @Volatile
    private var dispatcher: Dispatcher? = null

    @Volatile
    private var messageConsumer: MessageConsumer? = null

    @Blocking
    fun subscribe() {
        val connection = NatsConnectionManager.connection()
        val dispatcher = connection.createDispatcher()
        this.dispatcher = dispatcher

        for (method in descriptor.methods) {
            if (method.persistent) continue
            val invoker = invokers[method.index]
            val handler = MessageHandler { onRequest(method, invoker, it) }
            when (method.responders) {
                ResponderMode.ONE -> dispatcher.subscribe(method.subject, client.name, handler)
                ResponderMode.ALL -> dispatcher.subscribe(method.subject, handler)
            }
        }

        descriptor.streams?.let { streams ->
            runBlocking { JetStreamProvisioner.ensureStreams(streams) }
            val consumer = connection.getStreamContext(streams.stream)
                .createOrUpdateConsumer(JetStreamProvisioner.consumerConfig(client.name))
            messageConsumer = consumer.consume(dispatcher) { onPersistent(it) }
        }

        announcer?.start(dispatcher)

        connection.flush(5.seconds.toJavaDuration())
    }

    private fun onRequest(
        methodDescriptor: MethodDescriptor,
        invoker: ServiceInvoker,
        message: Message,
    ) {
        val headers = if (message.hasHeaders()) message.headers else null
        val contentType = headers?.getFirst(NatsHeaders.CONTENT_TYPE)
        val format = contentType?.let(WireFormat::fromContentType) ?: WireFormat.CBOR

        val timeout = headers?.getFirst(NatsHeaders.DEADLINE)?.toLongOrNull()
            ?.let { (it - System.currentTimeMillis()).milliseconds }
            ?: methodDescriptor.timeout

        if (!timeout.isPositive()) {
            client.logger.debug("Dropping expired request on ${message.subject}")
            return
        }

        val replyTo: String? = message.replyTo // null means fan-out without a response

        scope.launch(CoroutineName("surf-nats/${client.name}/rpc/${methodDescriptor.subject}")) {
            val reply = try {
                val completed = withTimeoutOrNull(timeout) {
                    val data = PayloadStore.resolve(
                        message,
                        PayloadBucket.TRANSIENT,
                        delete = methodDescriptor.responders == ResponderMode.ONE
                    )
                    val args = client.codec.decode(methodDescriptor.arguments, data, contentType)
                    Completed(invoker.call(args))
                } ?: return@launch // own timeout; do nothing requester has already given up
                encodeResult(methodDescriptor, completed.value, format)
            } catch (e: CancellationException) {
                ensureActive()
                errorReply(methodDescriptor, e, format)
            } catch (e: Exception) {
                errorReply(methodDescriptor, e, format)
            }

            if (replyTo != null) {
                send(methodDescriptor, replyTo, reply, format, timeout)
            }
        }
    }

    private fun onPersistent(message: Message) {
        val method = persistentBySubject[message.subject] ?: return message.term()
        val meta = message.metaData()
        val headers = if (message.hasHeaders()) message.headers else null
        val context = NatsMessageContext(
            messageId = headers?.getFirst(NatsJetStreamConstants.MSG_ID_HDR)
                ?: meta.streamSequence().toString(),
            deliveryCount = meta.deliveredCount(),
        )

        // Ignore already timed out messages
        val deadline = headers?.getFirst(NatsHeaders.DEADLINE)?.toLongOrNull()
        if (deadline != null && deadline <= System.currentTimeMillis()) return message.term()

        scope.launch(context + CoroutineName("surf-nats/${client.name}/persist/${method.subject}")) {
            val keepAlive = launch {
                while (isActive) {
                    delay(RpcDefaults.ACK_WAIT / 2)
                    message.inProgress()
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
        try {
            val data = PayloadStore.resolve(message, PayloadBucket.PERSISTENT, delete = false)
            val contentType =
                if (message.hasHeaders()) message.headers.getFirst(NatsHeaders.CONTENT_TYPE) else null
            invokers[method.index].call(
                client.codec.decode(
                    method.arguments,
                    data,
                    contentType,
                )
            )
        } catch (e: CancellationException) {
            currentCoroutineContext().ensureActive()
            return failFire(method, message, e)
        } catch (e: Exception) {
            return failFire(method, message, e)
        }

        message.ack()
        try {
            PayloadStore.delete(message, PayloadBucket.PERSISTENT)
        } catch (e: Exception) {
            client.logger.warn("Failed to delete payload of ${message.subject}", e)
        }
    }

    private suspend fun failFire(method: MethodDescriptor, message: Message, e: Throwable) {
        val delivered = message.metaData().deliveredCount()
        client.logger.error("Persistent ${method.subject} failed (delivery $delivered)", e)
        if (delivered >= RpcDefaults.MAX_DELIVER) {
            deadLetter(message, e)
            message.term()
        } else {
            val backoff = RpcDefaults.NAK_BACKOFF[(delivered - 1).toInt()
                .coerceIn(0, RpcDefaults.NAK_BACKOFF.lastIndex)]
            message.nakWithDelay(backoff.toJavaDuration())
        }
    }

    private suspend fun deadLetter(message: Message, e: Throwable) {
        val streams = checkNotNull(descriptor.streams)
        val headers = Headers(message.headers)
            .put(NatsHeaders.ERROR, e.javaClass.name)
            .put(NatsHeaders.DEAD_SUBJECT, message.subject)

        headers.remove(headers.keySet().filter { it.startsWith("Nats-", ignoreCase = true) })

        try {
            NatsConnectionManager.connection()
                .jetStream()
                .publishAsync(streams.dlqSubject, headers, message.data)
                .await()
        } catch (dlq: Exception) {
            client.logger.error("Failed to dead-letter ${message.subject}", dlq)
        }
    }

    private suspend fun handlePersistentRpc(
        method: MethodDescriptor,
        message: Message,
        deadline: Long?,
    ) {
        val headers = if (message.hasHeaders()) message.headers else null
        val replyTo = message.headers?.getFirst(NatsHeaders.REPLY_TO)
        val contentType = headers?.getFirst(NatsHeaders.CONTENT_TYPE)
        val format = contentType?.let(WireFormat::fromContentType) ?: WireFormat.CBOR
        val timeout =
            deadline?.let { (it - System.currentTimeMillis()).milliseconds } ?: method.timeout

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
        } catch (e: Exception) {
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

    private class Completed(@JvmField val value: Any?)
    private class Reply(@JvmField val headers: Headers, @JvmField val body: ByteArray)

    private fun encodeResult(
        method: MethodDescriptor,
        value: Any?,
        format: WireFormat,
    ): Reply {
        val headers = NatsHeaders.base(client.name, format)
        return when (val result = method.result) {
            ResultKind.Unit -> Reply(headers, EMPTY)
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
                    EMPTY
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

    companion object {
        private val EMPTY = ByteArray(0)
    }
}