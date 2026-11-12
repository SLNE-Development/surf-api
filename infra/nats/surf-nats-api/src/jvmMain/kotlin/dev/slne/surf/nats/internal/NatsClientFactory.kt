package dev.slne.surf.nats.internal

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.NatsClient

/**
 * Creates the clients behind `NatsClient.create`, implemented by surf-nats-core.
 */
@InternalNatsApi
public interface NatsClientFactory {

    public fun create(name: String): NatsClient

    public companion object {
        /**
         * The implementation that surf-nats-core registers as a `java.util.ServiceLoader` service.
         *
         * Initialization fails when surf-nats-core is not on the classpath.
         */
        public val instance: NatsClientFactory = requiredService<NatsClientFactory>()
    }
}