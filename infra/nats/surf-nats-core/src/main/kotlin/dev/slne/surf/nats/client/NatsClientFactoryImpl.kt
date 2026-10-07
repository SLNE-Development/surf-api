package dev.slne.surf.nats.client

import com.google.auto.service.AutoService
import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.internal.NatsClientFactory
import kotlinx.serialization.modules.SerializersModule
import java.util.concurrent.ConcurrentHashMap

@AutoService(NatsClientFactory::class)
@InternalNatsApi
class NatsClientFactoryImpl : NatsClientFactory {
    private val namePattern = NatsClient.NATS_CLIENT_NAME_PATTERN.toRegex()

    private val clients = ConcurrentHashMap<String, NatsClientImpl>()

    override fun create(
        name: String,
        serializersModule: SerializersModule
    ): NatsClient {
        require(namePattern.matches(name)) {
            "Invalid NATS client name '$name': must be kebab-case ([a-z0-9] separated by '-')"
        }

        return clients.compute(name) { _, existing ->
            check(existing == null) { "NATS client '$name' already exists" }
            NatsClientImpl(name, serializersModule, onClose = { clients.remove(name, it) })
        }!!
    }
}