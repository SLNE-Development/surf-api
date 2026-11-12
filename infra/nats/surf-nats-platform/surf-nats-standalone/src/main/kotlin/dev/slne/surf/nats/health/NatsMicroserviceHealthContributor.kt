package dev.slne.surf.nats.health

import com.google.auto.service.AutoService
import dev.slne.surf.microservice.api.health.MicroserviceHealthCheckResult
import dev.slne.surf.microservice.api.health.MicroserviceHealthContributor
import dev.slne.surf.microservice.api.health.MicroserviceHealthStatus
import dev.slne.surf.nats.NatsInstance

/**
 * Reports the state of the shared NATS connection to the microservice `health` command.
 *
 * The check is passive: it reads the connection status and sends nothing to the server. While a connection exists,
 * it returns one result for the component `nats` with the connection name as instance, healthy only while the
 * status is `CONNECTED` (reconnecting counts as unhealthy). Before start and after shutdown it returns an empty list.
 */
@AutoService(MicroserviceHealthContributor::class)
internal class NatsMicroserviceHealthContributor : MicroserviceHealthContributor {
    override val name: String = "nats"

    override suspend fun check(): List<MicroserviceHealthCheckResult> {
        val health = NatsInstance.instance.connectionHealth() ?: return emptyList()
        val instance = health.connectionName.takeIf { it.isNotBlank() }

        val result = if (health.connected) {
            MicroserviceHealthCheckResult(
                component = name,
                instance = instance,
                status = MicroserviceHealthStatus.HEALTHY,
            )
        } else {
            MicroserviceHealthCheckResult(
                component = name,
                instance = instance,
                status = MicroserviceHealthStatus.UNHEALTHY,
                message = "NATS connection is ${health.status.lowercase()}.",
            )
        }

        return listOf(result)
    }
}
