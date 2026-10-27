package dev.slne.surf.nats.js.client

import dev.slne.surf.nats.js.api.Answer
import dev.slne.surf.nats.js.api.ListenOptions
import dev.slne.surf.nats.js.api.ProtoMessage
import dev.slne.surf.nats.js.api.ProtoMessageInit
import dev.slne.surf.nats.js.codec.NatsHeaders
import dev.slne.surf.nats.js.codec.OutgoingHeaders
import dev.slne.surf.nats.js.event.EventHandler
import dev.slne.surf.nats.js.event.EventListeners
import dev.slne.surf.nats.js.event.EventPublisher
import dev.slne.surf.nats.js.event.NatsRegistration
import dev.slne.surf.nats.js.external.nats.core.NatsConnection
import dev.slne.surf.nats.js.external.nats.jetstream.JetStreamClient
import dev.slne.surf.nats.js.external.nats.jetstream.jetstream
import dev.slne.surf.nats.js.external.protobuf.DescMessage
import dev.slne.surf.nats.js.external.protobuf.DescMethod
import dev.slne.surf.nats.js.payload.PayloadStore
import dev.slne.surf.nats.js.rpc.RpcCaller
import dev.slne.surf.nats.runtime.ClientLifecycle
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.await
import kotlinx.coroutines.cancel

/**
 * One NATS connection of a surf-nats JS client, created by [NatsConnector].
 */
internal class NatsRuntime(
    val name: String,
    val origin: String,
    val connection: NatsConnection,
) {
    val scope = CoroutineScope(SupervisorJob())
    val payloads = PayloadStore(connection, scope)
    val jetStream: JetStreamClient by lazy { jetstream(connection) }

    private val lifecycle = ClientLifecycle(name)
    private val closed = CompletableDeferred<Unit>()
    private val baseHeaderBytes = NatsHeaders.encodedSize(NatsHeaders.base(origin))

    private val publisher = EventPublisher(this)
    private val listeners = EventListeners(this, lifecycle)
    private val rpc = RpcCaller(this)

    val isClosed: Boolean get() = lifecycle.isClosed

    init {
        scope.logConnectionStatus(connection, name)
    }

    fun headers(): OutgoingHeaders = OutgoingHeaders(NatsHeaders.base(origin), baseHeaderBytes)

    suspend fun publish(schema: DescMessage, event: ProtoMessageInit?, owner: String?, timeoutMs: Int?) {
        lifecycle.requireOpen()
        publisher.publish(schema, event, owner, timeoutMs)
    }

    suspend fun listen(
        schema: DescMessage,
        handler: EventHandler,
        owner: String?,
        options: ListenOptions?,
    ): NatsRegistration {
        lifecycle.requireAccepting()
        return listeners.listen(schema, handler, owner, options)
    }

    suspend fun call(method: DescMethod, request: ProtoMessageInit?, owner: String?, timeoutMs: Int?): ProtoMessage? {
        lifecycle.requireOpen()
        return rpc.call(method, request, owner, timeoutMs)
    }

    suspend fun callAnswer(method: DescMethod, request: ProtoMessageInit?, owner: String?, timeoutMs: Int?): Answer {
        lifecycle.requireOpen()
        return rpc.callAnswer(method, request, owner, timeoutMs)
    }

    /**
     * Drains the listeners and the connection; running handlers may still publish and call until the drain ends.
     * A second call waits for the first.
     */
    suspend fun close() {
        if (!lifecycle.beginClose()) return closed.await()
        try {
            listeners.drain()
            if (!connection.isClosed()) connection.drain().await()
        } finally {
            lifecycle.markClosed()
            scope.cancel("${lifecycle.label} closed")
            closed.complete(Unit)
        }
    }
}
