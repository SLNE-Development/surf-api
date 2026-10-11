package dev.slne.surf.nats.event

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.NatsRegistration
import dev.slne.surf.nats.bytecode.ListenerInvoker
import dev.slne.surf.nats.client.NatsClientImpl
import java.util.concurrent.atomic.AtomicBoolean

@InternalNatsApi
internal class EventListenerRegistration(
    private val client: NatsClientImpl,
    private val entries: List<Pair<ListenerMethod, ListenerInvoker>>
) : NatsRegistration {

    private val active = AtomicBoolean(true)
    override val isClosed: Boolean get() = !active.get()

    override fun close() {
        if (!active.compareAndSet(true, false)) return
        entries.forEach { (method, invoker) ->
            client.eventSubscriptions.remove(method, invoker)
        }
        client.unregisterRegistration(this)
    }
}