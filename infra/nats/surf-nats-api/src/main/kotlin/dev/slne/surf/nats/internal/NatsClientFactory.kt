package dev.slne.surf.nats.internal

import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.NatsClient
import kotlinx.serialization.modules.SerializersModule

@InternalNatsApi
interface NatsClientFactory {

    fun create(name: String, serializersModule: SerializersModule): NatsClient

    companion object {
        val instance = requiredService<NatsClientFactory>()
    }
}