package dev.slne.surf.nats.js.client

import dev.slne.surf.nats.js.api.ConnectOptions
import dev.slne.surf.nats.js.external.nats.core.ConnectionOptions
import dev.slne.surf.nats.js.external.nats.core.credsAuthenticator
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.runtime.ConnectRules
import kotlinx.coroutines.await
import node.crypto.randomUUID
import node.fs.ReadFileSyncBufferOptions
import node.fs.readFileSync
import kotlin.time.Duration.Companion.seconds
import dev.slne.surf.nats.js.external.nats.core.connect as natsConnect

internal object NatsConnector {
    /**
     * Opens one NATS connection; its name and `Surf-Origin` are `<client name>@<random instance id>`.
     */
    suspend fun connect(options: ConnectOptions): NatsRuntime {
        val settings = ConnectSettings.of(options)
        val origin = ProtocolHeaders.origin(settings.name, randomUUID())
        val connection = natsConnect(connectionOptions(settings, origin)).await()

        return NatsRuntime(settings.name, origin, connection)
    }

    private fun connectionOptions(settings: ConnectSettings, origin: String): ConnectionOptions {
        val options = ConnectionOptions(
            servers = settings.servers.toTypedArray(),
            name = origin,
            timeout = settings.connectTimeoutMs,
            maxReconnectAttempts = ConnectRules.MAX_RECONNECTS,
            pingInterval = ConnectRules.PING_INTERVAL_SECONDS.seconds.inWholeMilliseconds.toInt(),
        )

        return when (val auth = settings.auth) {
            null -> options
            is ConnectSettings.Auth.Token -> ConnectionOptions.copy(options, token = auth.token)
            is ConnectSettings.Auth.UserPass -> ConnectionOptions.copy(options, user = auth.user, pass = auth.pass)
            is ConnectSettings.Auth.CredsFile -> {
                val creds = readFileSync(auth.path, ReadFileSyncBufferOptions())
                ConnectionOptions.copy(options, authenticator = credsAuthenticator(creds))
            }
        }
    }
}
