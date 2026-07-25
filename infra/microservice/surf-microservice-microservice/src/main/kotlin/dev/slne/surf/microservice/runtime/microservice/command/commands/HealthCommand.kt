package dev.slne.surf.microservice.runtime.microservice.command.commands

import dev.slne.surf.microservice.api.microservice.command.microserviceCommand
import dev.slne.surf.microservice.api.microservice.health.MicroserviceHealthCheckResult
import dev.slne.surf.microservice.api.microservice.health.MicroserviceHealthStatus
import dev.slne.surf.microservice.runtime.microservice.command.MicroserviceCommandFailureException
import dev.slne.surf.microservice.runtime.microservice.health.MicroserviceHealthManager

internal fun healthCommand() = microserviceCommand("health") {
    val results = MicroserviceHealthManager.checkAll()
    val failures = results.filter { result ->
        result.status == MicroserviceHealthStatus.UNHEALTHY
    }

    if (failures.isEmpty()) {
        if (results.isEmpty()) {
            sendLine("healthy")
        } else {
            sendLine("healthy (${results.size} checks)")
        }

        return@microserviceCommand
    }

    throw MicroserviceCommandFailureException(
        buildString {
            appendLine("unhealthy")

            failures.forEach { failure ->
                append("- ")
                append(failure.displayName())
                append(": ")
                appendLine(failure.message ?: "Health check failed.")
            }
        }.trimEnd()
    )
}

private fun MicroserviceHealthCheckResult.displayName(): String {
    return buildString {
        append(component)

        instance?.let { instance ->
            append('[')
            append(instance)
            append(']')
        }
    }
}