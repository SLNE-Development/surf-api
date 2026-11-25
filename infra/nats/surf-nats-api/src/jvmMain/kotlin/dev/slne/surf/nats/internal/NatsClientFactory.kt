package dev.slne.surf.nats.internal

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.api.spring.surfBean
import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.NatsClient

/**
 * Creates the clients behind `NatsClient.create`, implemented by surf-nats-core.
 */
@InternalNatsApi
public interface NatsClientFactory {

    public fun create(name: String): NatsClient

    @InternalNatsApi
    public companion object {
        /**
         * The bean of surf-api's root context that surf-nats-core declares.
         *
         * @throws IllegalStateException if no root context is installed yet
         */
        @InternalSurfApi
        public val instance: NatsClientFactory by surfBean<NatsClientFactory>()
    }
}
