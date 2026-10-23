package dev.slne.surf.nats.internal

import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.NatsClient

@InternalNatsApi
public interface NatsClientFactory {

    public fun create(name: String): NatsClient

    public companion object {
        public val instance: NatsClientFactory = requiredService<NatsClientFactory>()
    }
}