package dev.slne.surf.nats.js.client

import dev.slne.surf.nats.js.api.ConnectOptions
import dev.slne.surf.nats.js.config.ConnectSettings
import dev.slne.surf.nats.js.connection.NatsConnector
import dev.slne.surf.nats.protocol.ProtocolHeaders
import node.crypto.randomUUID

internal object NatsClientFactory {
    /**
     * Connects a new client; its connection name and `Surf-Origin` are `<client name>@<random instance id>`.
     */
    suspend fun create(options: ConnectOptions): NatsClientImpl {
        val settings = ConnectSettings.of(options)
        val origin = ProtocolHeaders.origin(settings.name, randomUUID())
        val connection = NatsConnector.connect(settings, connectionName = origin)

        return NatsClientImpl(settings.name, origin, connection)
    }
}
