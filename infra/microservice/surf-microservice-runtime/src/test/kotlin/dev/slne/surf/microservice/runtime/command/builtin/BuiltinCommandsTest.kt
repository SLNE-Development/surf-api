package dev.slne.surf.microservice.runtime.command.builtin

import dev.slne.surf.microservice.api.command.MicroserviceCommand
import dev.slne.surf.microservice.api.command.MicroserviceCommandContext
import dev.slne.surf.microservice.api.command.MicroserviceCommandResult
import dev.slne.surf.microservice.api.command.MicroserviceCommandSource
import dev.slne.surf.microservice.api.health.MicroserviceHealthCheckResult
import dev.slne.surf.microservice.api.health.MicroserviceHealthContributor
import dev.slne.surf.microservice.api.health.MicroserviceHealthStatus
import dev.slne.surf.microservice.runtime.command.MicroserviceCommandDispatcher
import dev.slne.surf.microservice.runtime.command.RecordingCommandOutput
import dev.slne.surf.microservice.runtime.health.MicroserviceHealthManager
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BuiltinCommandsTest {
    private val output = RecordingCommandOutput()

    @Test
    fun `help lists name, aliases and description of every command sorted by name`() = runTest {
        val user = object : MicroserviceCommand("ban", "b", "block") {
            override suspend fun MicroserviceCommandContext.execute(args: List<String>) = Unit
        }
        val help = HelpCommand { listOf(ExitCommand(), user) }

        dispatch(help, "help")

        assertEquals(
            listOf("Commands:", "  ban (b, block)", "  exit (quit) - Shuts down the microservice."),
            output.lines,
        )
    }

    @Test
    fun `exit requests a shutdown and answers`() = runTest {
        val dispatch = dispatch(ExitCommand(), "quit")

        assertTrue(dispatch.shutdownRequested)
        assertEquals(1, output.lines.size)
    }

    @Test
    fun `health reports healthy checks`() = runTest {
        val health = HealthCommand(healthManager(healthy("database"), healthy("nats")))

        val dispatch = dispatch(health, "health")

        assertEquals(MicroserviceCommandResult.Success, dispatch.result)
        assertEquals(listOf("healthy (2 checks)"), output.lines)
    }

    @Test
    fun `health fails with every unhealthy check`() = runTest {
        val unhealthy = MicroserviceHealthCheckResult(
            component = "database",
            instance = "main",
            status = MicroserviceHealthStatus.UNHEALTHY,
            message = "connection refused",
        )
        val health = HealthCommand(healthManager(healthy("nats"), unhealthy))

        val dispatch = dispatch(health, "health")

        assertEquals(MicroserviceCommandResult.Failure("unhealthy\n- database[main]: connection refused"), dispatch.result)
    }

    private suspend fun dispatch(command: MicroserviceCommand, line: String) =
        MicroserviceCommandDispatcher { label -> command.takeIf { it.matches(label) } }
            .dispatch(line, MicroserviceCommandSource.CONSOLE, output)

    private fun healthManager(vararg results: MicroserviceHealthCheckResult) = MicroserviceHealthManager {
        listOf(
            object : MicroserviceHealthContributor {
                override val name = "fixed"

                override suspend fun check() = results.toList()
            },
        )
    }

    private fun healthy(component: String) =
        MicroserviceHealthCheckResult(component = component, status = MicroserviceHealthStatus.HEALTHY)
}
