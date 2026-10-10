package dev.slne.surf.nats.internal

import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.NatsClient
import kotlinx.serialization.modules.SerializersModule

@InternalNatsApi
public interface NatsClientFactory {

    public fun create(name: String, serializersModule: SerializersModule): NatsClient

    public companion object {
        public val instance: NatsClientFactory = requiredService<NatsClientFactory>()
    }
}