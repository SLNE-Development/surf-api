package dev.slne.surf.nats.js.client

import dev.slne.surf.nats.descriptor.NatsResultKind
import dev.slne.surf.nats.js.api.Answer
import dev.slne.surf.nats.js.api.ListenOptions
import dev.slne.surf.nats.js.api.ProtoMessage
import dev.slne.surf.nats.js.api.ProtoMessageInit
import dev.slne.surf.nats.js.codec.NatsCodec
import dev.slne.surf.nats.js.codec.NatsHeaders
import dev.slne.surf.nats.js.connection.NatsConnector
import dev.slne.surf.nats.js.error.NatsCallTimeoutError
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
import dev.slne.surf.nats.js.interop.positiveTimeoutOrNull
import dev.slne.surf.nats.js.rpc.MethodDescriptor
import dev.slne.surf.nats.js.rpc.RpcCaller
import dev.slne.surf.nats.js.rpc.payload.PayloadStore
import dev.slne.surf.nats.protocol.ProtocolDefaults
import dev.slne.surf.nats.runtime.AbstractNatsClient
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.seconds

/**
 * One NATS connection of a surf-nats JS client, created by [NatsClientFactory].
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
     * Publishes the event and waits for the flush that confirms it; events are never offloaded.
     */
    suspend fun publish(schema: DescMessage, event: ProtoMessageInit?, owner: String?, timeoutMs: Int?) {
        checkOpen()
        val descriptor = EventDescriptor.of(schema)
        val subject = descriptor.subject(owner)
        val timeout = positiveTimeoutOrNull(timeoutMs, "Timeout") ?: ProtocolDefaults.CORE_TIMEOUT_SECONDS.seconds
        val data = NatsCodec.encode(schema, event)
        val header = headers()

        requireEventFits(
            descriptor.typeName,
            subject,
            data.length + NatsHeaders.serializedLength(header),
            payloads.maxPayload,
        )

        connection.publish(subject, data, NatsPublishOptions(headers = header))
        withTimeoutOrNull(timeout) { // TODO: replace with fire and forget like on the jvm?
            connection.flush().await()
        } ?: throw NatsCallTimeoutError(subject, timeout.inWholeMilliseconds.toInt())
    }

    suspend fun registerListener(
        schema: DescMessage,
        handler: EventHandler,
        owner: String?,
        options: ListenOptions?,
    ): NatsRegistration {
        lifecycle.requireAccepting()
        val descriptor = EventDescriptor.of(schema)
        val subject = descriptor.subject(owner)
        val settings = ListenerSettings.of(options)
        require(settings.problem == null) {
            "${settings.problem} (listener for ${descriptor.typeName} on $subject)"
        }

        val listener = EventListener(
            descriptor,
            subject,
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
     * Calls every method except `ALL_INSTANCES` methods with a result; returns `undefined` for UNIT methods.
     */
    suspend fun call(method: DescMethod, request: ProtoMessageInit?, owner: String?, timeoutMs: Int?): ProtoMessage? {
        checkOpen()
        val descriptor = MethodDescriptor.of(method)
        require(descriptor.result != NatsResultKind.ANSWER) {
            "${descriptor.label} is an ALL_INSTANCES method with a result and returns an Answer: use callAnswer"
        }

        return rpcCaller.call(descriptor, owner, request, timeoutMs).unsafeCast<ProtoMessage?>()
    }

    /**
     * Calls an `ALL_INSTANCES` method with a result.
     */
    suspend fun callAnswer(method: DescMethod, request: ProtoMessageInit?, owner: String?, timeoutMs: Int?): Answer {
        checkOpen()
        val descriptor = MethodDescriptor.of(method)
        require(descriptor.result == NatsResultKind.ANSWER) {
            "${descriptor.label} is not an ALL_INSTANCES method with a result: use call"
        }

        return rpcCaller.call(descriptor, owner, request, timeoutMs).unsafeCast<Answer>()
    }

    override fun closeRegistration(registration: NatsRegistration) = registration.close()

    /**
     * Drains the subscriptions and the connection; running handlers may still publish and call until the drain
     * ends. A second call waits for the first.
     */
    suspend fun close() {
        if (!lifecycle.beginClose()) return closed.await()
        try {
            // Running handlers may still publish and call through this client while their subscriptions drain
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
