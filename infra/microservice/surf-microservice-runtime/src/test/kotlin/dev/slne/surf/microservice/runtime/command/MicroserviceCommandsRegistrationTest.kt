package dev.slne.surf.microservice.runtime.command

import dev.slne.surf.microservice.api.command.MicroserviceCommand
import dev.slne.surf.microservice.api.command.MicroserviceCommandContext
import dev.slne.surf.microservice.api.command.MicroserviceCommands
import dev.slne.surf.microservice.runtime.command.builtin.registerBuiltinCommands
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Checks that registering a command only if its labels are free cannot be raced by another registration.
 */
class MicroserviceCommandsRegistrationTest {

    @Test
    fun `registerIfAbsent registers a free command and reports the first taken label of a taken one`() {
        val first = command("if-absent-first", "if-absent-alias")
        val second = command("if-absent-second", "IF-ABSENT-ALIAS")

        assertNull(MicroserviceCommands.registerIfAbsent(first))
        assertEquals("IF-ABSENT-ALIAS", MicroserviceCommands.registerIfAbsent(second))

        assertSame(first, MicroserviceCommands.find("if-absent-alias"))
        assertNull(MicroserviceCommands.find("if-absent-second"))
    }

    @Test
    fun `concurrent built-in registrations of one label register exactly one command`() {
        repeat(RACES) { race ->
            val label = "race-$race"
            val start = CyclicBarrier(CONTENDERS)
            val executor = Executors.newFixedThreadPool(CONTENDERS)
            try {
                val results = List(CONTENDERS) { command(label) }.map { contender ->
                    executor.submit<Map<MicroserviceCommand, String>> {
                        start.await()
                        registerBuiltinCommands(listOf(contender))
                    }
                }.map { it.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) }

                assertEquals(CONTENDERS - 1, results.count { it.isNotEmpty() })
                assertEquals(1, MicroserviceCommands.all.count { it.matches(label) })
            } finally {
                executor.shutdownNow()
            }
        }
    }

    private fun command(name: String, vararg aliases: String): MicroserviceCommand =
        object : MicroserviceCommand(name, *aliases) {
            override suspend fun MicroserviceCommandContext.execute(args: List<String>) = Unit
        }

    private companion object {
        const val RACES = 50
        const val CONTENDERS = 8
        const val TIMEOUT_SECONDS = 10L
    }
}
