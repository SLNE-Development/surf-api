package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.*
import dev.slne.surf.nats.bytecode.ServiceInvoker
import dev.slne.surf.nats.bytecode.ServiceInvokerFactory
import dev.slne.surf.nats.bytecode.call
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.codec.NatsHeaders
import dev.slne.surf.nats.connection.NatsConnectionManager
import io.nats.client.Dispatcher
import io.nats.client.Message
import io.nats.client.MessageHandler
import io.nats.client.impl.Headers
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
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

    private val active = AtomicBoolean(true)
    override val isActive get() = active.get()

    @Volatile
    private var dispatcher: Dispatcher? = null

    fun subscribe() {
        val dispatcher = NatsConnectionManager.connection().createDispatcher()
        this.dispatcher = dispatcher

        for (method in descriptor.methods) {
            val invoker = invokers[method.index]
            val handler = MessageHandler { onRequest(method, invoker, it) }
            when (method.responders) {
                ResponderMode.ONE -> dispatcher.subscribe(method.subject, client.name, handler)
                ResponderMode.ALL -> dispatcher.subscribe(method.subject, handler)
            }
        }

        announcer?.start(dispatcher)

        NatsConnectionManager.connection().flush(5.seconds.toJavaDuration())
    }

    override fun close() {
        if (!active.compareAndSet(true, false)) return
        announcer?.close()
        dispatcher?.let {
            try {
                NatsConnectionManager.connection().closeDispatcher(it)
            } catch (e: Exception) {
                client.logger.warn("Failed to close service dispatcher", e)
            }
        }
        client.unregisterService(this)
        client.unregisterRegistration(this)
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
            ?: RpcDefaults.CORE_TIMEOUT

        if (!timeout.isPositive()) {
            client.logger.debug("Dropping expired request on ${message.subject}")
            return
        }

        val replyTo: String? = message.replyTo // null means fan-out without a response

        client.clientScope.launch(CoroutineName("surf-nats/${client.name}/rpc/${methodDescriptor.subject}")) {
            val reply = try {
                val args =
                    client.codec.decode(methodDescriptor.arguments, message.data, contentType)
                val completed = withTimeoutOrNull(timeout) { Completed(invoker.call(args)) }
                    ?: return@launch // own timeout; do nothing requester has already given up
                encodeResult(methodDescriptor, completed.value, format)
            } catch (e: CancellationException) {
                ensureActive()
                errorReply(methodDescriptor, e, format)
            } catch (e: Exception) {
                errorReply(methodDescriptor, e, format)
            }

            if (replyTo != null) {
                send(methodDescriptor, replyTo, reply, format)
            }
        }
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

    private fun send(
        method: MethodDescriptor,
        replyTo: String,
        reply: Reply,
        format: WireFormat,
    ) {
        val connection = NatsConnectionManager.connection()
        try {
            connection.publish(replyTo, reply.headers, reply.body)
        } catch (e: IllegalArgumentException) { // Most likely too large, replace with object store
            val fallback = errorReply(method, e, format)
            connection.publish(replyTo, fallback.headers, fallback.body)
        }
    }

    companion object {
        private val EMPTY = ByteArray(0)
    }
}