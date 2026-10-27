package dev.slne.surf.nats.js.client

import dev.slne.surf.nats.js.external.nats.core.NatsConnection
import js.iterable.asFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Logs disconnects, errors and reconnects of [connection] until the connection closes.
 */
internal fun CoroutineScope.logConnectionStatus(
    connection: NatsConnection,
    clientName: String,
) = launch {
    connection.status()
        .asFlow()
        .collect { status ->
            when (val type = status.type) {
                "disconnect", "error", "staleConnection", "slowConsumer" -> {
                    console.warn(
                        "surf-nats[$clientName]: NATS connection $type",
                        status.server ?: status.error ?: ""
                    )
                }

                "reconnect" -> {
                    console.info(
                        "surf-nats[$clientName]: NATS connection reconnect",
                        status.server
                    )
                }
            }
        }
}
