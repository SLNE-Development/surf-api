package dev.slne.surf.api.nats

import dev.slne.surf.nats.embedded.EmbeddedNats
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator

/**
 * Reports the state of the shared NATS connection.
 *
 * The check is passive: it reads the connection status and sends nothing to the server. It is up while the
 * connection is `CONNECTED` and down otherwise, reconnecting included. Without a connection, because nothing of the
 * context uses NATS or it is not connected yet, it is up with the detail `connection: none`.
 */
internal class NatsHealthIndicator : HealthIndicator {

    @Suppress("FoldInitializerAndIfToElvis")
    override fun health(): Health {
        val connection = EmbeddedNats.connectionHealth()

        if (connection == null) {
            return Health.up()
                .withDetail(CONNECTION_DETAIL, NO_CONNECTION)
                .build()
        }


        if (connection.connected) {
            return Health.up()
                .withDetail(CONNECTION_DETAIL, connection.connectionName)
                .build()
        }

        return Health.down()
            .withDetail(CONNECTION_DETAIL, connection.connectionName)
            .withDetail(ERROR_DETAIL, "NATS connection is ${connection.status.lowercase()}.")
            .build()
    }

    private companion object {
        const val CONNECTION_DETAIL = "connection"
        const val ERROR_DETAIL = "error"
        const val NO_CONNECTION = "none"
    }
}
