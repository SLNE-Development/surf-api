package dev.slne.surf.nats.js.connection

import dev.slne.surf.nats.js.client.ClientLogger
import dev.slne.surf.nats.js.config.ConnectSettings
import dev.slne.surf.nats.js.external.nats.core.ConnectionOptions
import dev.slne.surf.nats.js.external.nats.core.NatsConnection
import dev.slne.surf.nats.js.external.nats.core.credsAuthenticator
import dev.slne.surf.nats.runtime.ConnectRules
import js.iterable.asFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import node.fs.ReadFileSyncBufferOptions
import node.fs.readFileSync
import kotlin.time.Duration.Companion.seconds
import dev.slne.surf.nats.js.external.nats.core.connect as natsConnect

/**
 * Opens the NATS connection of a client with the options of the JVM `NatsConnectionManager`.
 */
internal object NatsConnector {

    suspend fun connect(settings: ConnectSettings, connectionName: String): NatsConnection {
        return natsConnect(connectionOptions(settings, connectionName)).await()
    }

    /**
     * Logs disconnects, errors and reconnects of [connection] until it closes.
     */
    fun logStatus(
        scope: CoroutineScope,
        connection: NatsConnection,
        logger: ClientLogger,
    ): Job = scope.launch {
        connection.status()
            .asFlow()
            .collect { status ->
                when (val type = status.type) {
                    "disconnect", "error", "staleConnection", "slowConsumer" -> {
                        logger.warn(
                            "NATS connection $type",
                            status.server ?: status.error ?: ""
                        )
                    }

                    "reconnect" -> logger.info("NATS connection reconnect: ${status.server}")
                }
            }
    }

    private fun connectionOptions(
        settings: ConnectSettings,
        connectionName: String,
    ): ConnectionOptions {
        val options = ConnectionOptions(
            servers = settings.servers.toTypedArray(),
            name = connectionName,
            timeout = settings.connectTimeoutMs,
            maxReconnectAttempts = ConnectRules.MAX_RECONNECTS,
            pingInterval = ConnectRules.PING_INTERVAL_SECONDS.seconds.inWholeMilliseconds.toInt(),
        )

        return when (val auth = settings.auth) {
            null -> options
            is ConnectSettings.Auth.Token -> ConnectionOptions.copy(options, token = auth.token)
            is ConnectSettings.Auth.UserPass -> ConnectionOptions.copy(
                options,
                user = auth.user,
                pass = auth.pass
            )

            is ConnectSettings.Auth.CredsFile -> {
                val creds = readFileSync(auth.path, ReadFileSyncBufferOptions())
                ConnectionOptions.copy(options, authenticator = credsAuthenticator(creds))
            }
        }
    }
}
