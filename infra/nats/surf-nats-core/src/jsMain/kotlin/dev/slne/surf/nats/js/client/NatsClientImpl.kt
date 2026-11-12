package dev.slne.surf.nats.js.client

import dev.slne.surf.nats.descriptor.NatsResultKind
import dev.slne.surf.nats.js.api.Answer
import dev.slne.surf.nats.js.api.CallOptions
import dev.slne.surf.nats.js.api.ListenOptions
import dev.slne.surf.nats.js.api.ProtoMessage
import dev.slne.surf.nats.js.api.ProtoMessageInit
import dev.slne.surf.nats.js.codec.NatsCodec
import dev.slne.surf.nats.js.codec.NatsHeaders
import dev.slne.surf.nats.js.connection.NatsConnector
import dev.slne.surf.nats.js.event.EventDescriptor
import dev.slne.surf.nats.js.event.EventHandler
import dev.slne.surf.nats.js.event.EventListener
import dev.slne.surf.nats.js.event.EventSubscriptions
import dev.slne.surf.nats.js.event.ListenerSettings
import dev.slne.surf.nats.js.event.NatsRegistration
import dev.slne.surf.nats.js.external.nats.core.MsgHdrs
import dev.slne.surf.nats.js.external.nats.core.NatsConnection
import dev.slne.surf.nats.js.external.nats.core.NatsPublishOptions
import dev.slne.surf.nats.js.external.nats.jetstream.JetStreamClient
import dev.slne.surf.nats.js.external.nats.jetstream.jetstream
import dev.slne.surf.nats.js.external.protobuf.DescMessage
import dev.slne.surf.nats.js.external.protobuf.DescMethod
import dev.slne.surf.nats.js.rpc.MethodDescriptor
import dev.slne.surf.nats.js.rpc.RpcCaller
import dev.slne.surf.nats.js.rpc.payload.PayloadStore
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.runtime.AbstractNatsClient
import dev.slne.surf.nats.runtime.randomId
import kotlinx.coroutines.*

/**
 * One NATS connection of a surf-nats JS client, created by [NatsClientFactory] and exposed as `SurfNatsClient`.
 *
 * Owns the event subscriptions, the RPC caller and the payload store of the connection. [clientScope] runs the
 * background work and is cancelled when [close] ends.
 */
internal class NatsClientImpl(
    name: String,
    val origin: String,
    val connection: NatsConnection,
) : AbstractNatsClient<NatsRegistration>(name) {

    internal val clientScope = CoroutineScope(
        CoroutineName("surf-nats/$name") +
                SupervisorJob() +
                CoroutineExceptionHandler { context, throwable ->
                    val coroutine = context[CoroutineName]?.name ?: "surf-nats/$name"
                    logger.error("Unhandled exception in coroutine '$coroutine'", throwable)
                }
    )

    internal val eventSubscriptions = EventSubscriptions(this)

    private val rpcCaller = RpcCaller(this)

    private val closed = CompletableDeferred<Unit>()

    val payloads = PayloadStore(connection, clientScope)

    val jetStream: JetStreamClient by lazy { jetstream(connection) }

    init {
        NatsConnector.logStatus(clientScope, connection, logger)
    }

    internal fun headers(): MsgHdrs = NatsHeaders.forOrigin(origin)

    /**
     * Publishes the event under [eventId], a random UUID if `null`, without waiting for the server.
     *
     * Events are never offloaded. The connection buffers the message, and [close] drains the buffer before the
     * connection ends. Every failure is thrown before anything is sent.
     */
    fun publish(schema: DescMessage, event: ProtoMessageInit?, eventId: String?) {
        checkOpen()
        val id = eventId ?: randomId()
        ProtocolHeaders.eventIdProblem(id)?.let { throw IllegalArgumentException(it) }
        val descriptor = EventDescriptor.of(schema)
        val subject = descriptor.subject
        val data = NatsCodec.encode(schema, event)
        val header = headers()
        header.set(ProtocolHeaders.EVENT_ID, id)

        requireEventFits(
            descriptor.typeName,
            subject,
            data.length + NatsHeaders.serializedLength(header),
            payloads.maxPayload,
        )

        connection.publish(subject, data, NatsPublishOptions(headers = header))
    }

    /**
     * Subscribes [handler] and returns after a flush, so the server knows the subscription.
     *
     * A failed subscribe or flush removes the listener again before the failure is rethrown.
     */
    suspend fun registerListener(
        schema: DescMessage,
        handler: EventHandler,
        options: ListenOptions?,
    ): NatsRegistration {
        lifecycle.requireAccepting()
        val descriptor = EventDescriptor.of(schema)
        val subject = descriptor.subject
        val settings = ListenerSettings.of(options)
        require(settings.problem == null) {
            "${settings.problem} (listener for ${descriptor.typeName} on $subject)"
        }

        val listener = EventListener(
            descriptor,
            settings.queueGroup(name),
            settings.ignoreFromThisClient,
            handler,
        )

        try {
            eventSubscriptions.add(listener)
            flush()
        } catch (e: Throwable) {
            try {
                eventSubscriptions.remove(listener)
            } catch (cleanup: Throwable) {
                e.addSuppressed(cleanup)
            }
            throw e
        }

        return NatsRegistration(this, listener).also { registerRegistration(it) }
    }

    /**
     * Calls every method except `ALL_INSTANCES` methods with a result and returns `undefined` for UNIT methods.
     */
    suspend fun call(method: DescMethod, request: ProtoMessageInit?, options: CallOptions?): ProtoMessage? {
        checkOpen()
        val descriptor = MethodDescriptor.of(method)
        require(descriptor.result != NatsResultKind.ANSWER) {
            "${descriptor.label} is an ALL_INSTANCES method with a result and returns an Answer: use callAnswer"
        }

        return rpcCaller.call(descriptor, request, options).unsafeCast<ProtoMessage?>()
    }

    /**
     * Calls an `ALL_INSTANCES` method with a result.
     */
    suspend fun callAnswer(method: DescMethod, request: ProtoMessageInit?, options: CallOptions?): Answer {
        checkOpen()
        val descriptor = MethodDescriptor.of(method)
        require(descriptor.result == NatsResultKind.ANSWER) {
            "${descriptor.label} is not an ALL_INSTANCES method with a result: use call"
        }

        return rpcCaller.call(descriptor, request, options).unsafeCast<Answer>()
    }

    override fun closeRegistration(registration: NatsRegistration) = registration.close()

    /**
     * Drains the subscriptions, then the connection. Running handlers may still publish and call until the drain
     * ends. A second call waits for the first.
     */
    suspend fun close() {
        if (!lifecycle.beginClose()) return closed.await()
        try {
            eventSubscriptions.drain()
            closeRegistrations()
            if (!connection.isClosed()) connection.drain().await()
        } finally {
            lifecycle.markClosed()
            clientScope.cancel("${lifecycle.label} closed")
            closed.complete(Unit)
        }
    }

    internal suspend fun flush() {
        connection.flush().await()
    }
}
