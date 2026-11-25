package dev.slne.surf.api.nats

import dev.slne.surf.nats.connection.NatsConnectionHealth
import dev.slne.surf.nats.embedded.EmbeddedNats
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.boot.health.contributor.AbstractHealthIndicator
import org.springframework.boot.health.contributor.Health

/**
 * A bean definition that creates a [NatsHealthIndicator] for [nats].
 *
 * Kept out of [NatsContextContributor]: Spring introspects the declared methods of that bean, and
 * the supplier's synthetic method returns [NatsHealthIndicator], which fails to load without Spring
 * Boot on the classpath.
 *
 * @param nats the NATS state of the context the indicator belongs to
 * @return the definition of the indicator bean
 */
internal fun natsHealthIndicatorDefinition(nats: NatsPluginContext): RootBeanDefinition {
    return RootBeanDefinition(NatsHealthIndicator::class.java) { NatsHealthIndicator(nats) }
}

/**
 * Reports the state of the shared NATS connection for one plugin context.
 *
 * The check is passive: it reads the connection status and sends nothing to the server.
 *
 * The status is `UNKNOWN` while the context has no [dev.slne.surf.nats.NatsClient], which is the
 * case for a plugin that uses nothing of NATS. With a client it is `UP` while the connection is
 * `CONNECTED` and `DOWN` otherwise, reconnecting and not yet connected included.
 *
 * @param nats the NATS state of the context the indicator belongs to
 * @param connectionHealth reads the state of the shared connection
 */
internal class NatsHealthIndicator(
    private val nats: NatsPluginContext,
    private val connectionHealth: () -> NatsConnectionHealth? = EmbeddedNats::connectionHealth,
) : AbstractHealthIndicator("NATS health check failed") {

    override fun doHealthCheck(builder: Health.Builder) {
        if (!nats.hasClient()) {
            builder.unknown().withDetail(CONNECTION_DETAIL, NO_CLIENT)

            return
        }

        val connection = connectionHealth()

        if (connection == null) {
            builder.down()
                .withDetail(CONNECTION_DETAIL, NO_CONNECTION)
                .withDetail(ERROR_DETAIL, "NATS client is not connected yet.")

            return
        }

        if (connection.connected) {
            builder.up().withDetail(CONNECTION_DETAIL, connection.connectionName)

            return
        }

        builder.down()
            .withDetail(CONNECTION_DETAIL, connection.connectionName)
            .withDetail(ERROR_DETAIL, "NATS connection is ${connection.status.lowercase()}.")
    }

    private companion object {
        const val CONNECTION_DETAIL = "connection"
        const val ERROR_DETAIL = "error"
        const val NO_CLIENT = "no client"
        const val NO_CONNECTION = "none"
    }
}
