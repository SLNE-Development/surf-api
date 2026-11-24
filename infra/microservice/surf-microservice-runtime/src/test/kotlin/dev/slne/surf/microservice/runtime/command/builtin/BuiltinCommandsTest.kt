package dev.slne.surf.microservice.runtime.command.builtin

import dev.slne.surf.microservice.api.command.*
import dev.slne.surf.microservice.runtime.command.MicroserviceCommandDispatcher
import dev.slne.surf.microservice.runtime.command.RecordingCommandOutput
import dev.slne.surf.microservice.runtime.health.MicroserviceHealth
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.springframework.boot.health.contributor.CompositeHealthContributor
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthContributor
import org.springframework.boot.health.contributor.HealthContributors
import org.springframework.boot.health.contributor.HealthIndicator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
    fun `health reports healthy checks`() = runBlocking {
        val health = HealthCommand(health("database" to up(), "nats" to up()))

        val dispatch = dispatch(health, "health")

        assertEquals(MicroserviceCommandResult.Success, dispatch.result)
        assertEquals(listOf("healthy (2 checks)"), output.lines)
    }

    @Test
    fun `health fails with every unhealthy check`() = runBlocking {
        val database = CompositeHealthContributor.fromMap(
            mapOf(
                "main" to HealthIndicator {
                    Health.down().withDetail(
                        "error",
                        "connection refused",
                    ).build()
                },
            ),
        )
        val health = HealthCommand(health("nats" to up(), "database" to database))

        val dispatch = dispatch(health, "health")

        assertEquals(
            MicroserviceCommandResult.Failure("unhealthy\n- database[main]: connection refused"),
            dispatch.result,
        )
    }

    @Test
    fun `a built-in is skipped when one of its aliases is already registered`() {
        MicroserviceCommands.register(command("registration-taken"))
        val builtin = command("registration-builtin", "registration-alias", "registration-taken")
        val free = command("registration-free")

        val skipped = registerBuiltinCommands(listOf(builtin, free))

        assertEquals(mapOf(builtin to "registration-taken"), skipped)
        assertNull(MicroserviceCommands.find("registration-builtin"))
        assertNull(MicroserviceCommands.find("registration-alias"))
        assertNotNull(MicroserviceCommands.find("registration-free"))
    }

    private fun command(name: String, vararg aliases: String): MicroserviceCommand {
        return object :
            MicroserviceCommand(name, *aliases) {
            override suspend fun MicroserviceCommandContext.execute(args: List<String>) = Unit
        }
    }

    private suspend fun dispatch(
        command: MicroserviceCommand,
        line: String,
    ) = MicroserviceCommandDispatcher { label -> command.takeIf { it.matches(label) } }
        .dispatch(line, MicroserviceCommandSource.CONSOLE, output)

    private fun health(
        vararg contributors: Pair<String, HealthContributor>,
    ) = MicroserviceHealth(
        { HealthContributors.of(CompositeHealthContributor.fromMap(contributors.toMap())) },
    )

    private fun up() = HealthIndicator { Health.up().build() }
}
