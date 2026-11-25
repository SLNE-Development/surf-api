package dev.slne.surf.nats.client

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.internal.NatsClientFactory
import dev.slne.surf.nats.protocol.ProtocolNaming
import java.util.concurrent.ConcurrentHashMap

/**
 * Creates the `NatsClient`s of this process and tracks the open ones by name.
 *
 * A name is free again once its client is closed. Thread-safe.
 */
@InternalNatsApi
internal class NatsClientFactoryImpl : NatsClientFactory {
    private val clients = ConcurrentHashMap<String, NatsClientImpl>()

    /**
     * Creates a client named [name].
     *
     * @throws IllegalArgumentException if [name] is not a valid client name
     * @throws IllegalStateException if an open client already has that name
     */
    override fun create(name: String): NatsClient {
        ProtocolNaming.requireClientName(name)

        return clients.compute(name) { _, existing ->
            check(existing == null) { "NATS client '$name' already exists" }

            NatsClientImpl(name, onClose = { clients.remove(name, it) })
        }!!
    }
}
