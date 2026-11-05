package dev.slne.surf.microservice.runtime.command.builtin

import dev.slne.surf.microservice.api.command.MicroserviceCommand
import dev.slne.surf.microservice.api.command.MicroserviceCommandContext
import dev.slne.surf.microservice.api.health.MicroserviceHealthCheckResult
import dev.slne.surf.microservice.api.health.MicroserviceHealthStatus
import dev.slne.surf.microservice.runtime.health.MicroserviceHealthManager

/**
 * Reports readiness; fails with one line per unhealthy result, which makes `microservicectl health` exit with `1`.
 */
internal class HealthCommand(
    private val healthManager: MicroserviceHealthManager,
) : MicroserviceCommand("health") {
    override val description = "Checks the readiness of all components."

    override suspend fun MicroserviceCommandContext.execute(args: List<String>) {
        val results = healthManager.checkAll()
        val failures = results.filter { result -> result.status == MicroserviceHealthStatus.UNHEALTHY }

        if (failures.isNotEmpty()) fail(describeFailures(failures))
        sendLine(if (results.isEmpty()) "healthy" else "healthy (${results.size} checks)")
    }

    private fun describeFailures(failures: List<MicroserviceHealthCheckResult>) = buildString {
        append("unhealthy")
        failures.forEach { failure -> append("\n- ${failure.displayName()}: ${failure.message}") }
    }

    private fun MicroserviceHealthCheckResult.displayName(): String =
        if (instance == null) component else "$component[$instance]"
}
