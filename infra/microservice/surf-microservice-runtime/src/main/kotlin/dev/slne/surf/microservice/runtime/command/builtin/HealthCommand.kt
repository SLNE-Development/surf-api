package dev.slne.surf.microservice.runtime.command.builtin

import dev.slne.surf.microservice.api.command.MicroserviceCommand
import dev.slne.surf.microservice.api.command.MicroserviceCommandContext
import dev.slne.surf.microservice.runtime.health.HealthCheckResult
import dev.slne.surf.microservice.runtime.health.MicroserviceHealth

/**
 * Reports readiness. Fails with one line per unhealthy result, which makes `microservicectl health` exit with `1`.
 */
internal class HealthCommand(
    private val health: MicroserviceHealth,
) : MicroserviceCommand("health") {
    override val description = "Checks the readiness of all components."

    override suspend fun MicroserviceCommandContext.execute(args: List<String>) {
        val results = health.checkAll()
        val failures = results.filterNot { it.healthy }

        if (failures.isNotEmpty()) fail(describeFailures(failures))
        sendLine(if (results.isEmpty()) "healthy" else "healthy (${results.size} checks)")
    }

    private fun describeFailures(failures: List<HealthCheckResult>) = buildString {
        append("unhealthy")
        failures.forEach { failure -> append("\n- ${failure.displayName}: ${failure.message}") }
    }
}
