package dev.slne.surf.nats

import com.google.auto.service.AutoService
import dev.slne.surf.nats.internal.NatsClientFactory
import kotlinx.serialization.modules.SerializersModule

@AutoService(NatsClientFactory::class)
class NatsClientFactoryImpl: NatsClientFactory {
    override fun create(
        name: String,
        serializersModule: SerializersModule
    ): NatsClient {

    }
}