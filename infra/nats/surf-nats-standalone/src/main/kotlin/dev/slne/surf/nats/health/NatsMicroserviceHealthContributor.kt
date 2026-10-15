package dev.slne.surf.nats.health

import com.google.auto.service.AutoService
import dev.slne.surf.microservice.api.microservice.health.MicroserviceHealthCheckResult
import dev.slne.surf.microservice.api.microservice.health.MicroserviceHealthContributor
import dev.slne.surf.microservice.api.microservice.health.MicroserviceHealthStatus
import dev.slne.surf.nats.NatsInstance

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
