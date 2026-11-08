package dev.slne.surf.api.minestom.server.impl.command

import dev.slne.surf.api.minestom.command.CommandAPICommand
import dev.slne.surf.api.minestom.command.CommandTree
import dev.slne.surf.api.minestom.command.dsl.anyExecutor
import dev.slne.surf.api.minestom.command.exception.CommandValidationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class CommandLabelValidationTest {

    @Test
    fun `a label may be written in any script`() {
        val definition = CommandAPICommand("überweisen")
            .withAliases("bezahlen", "παραλαβή")
            .anyExecutor { _, _ -> }
            .toDefinition()

        assertEquals("überweisen", definition.name)
        assertEquals(setOf("bezahlen", "παραλαβή"), definition.aliases)
    }

    @Test
    fun `a tree label may be written in any script`() {
        val definition = CommandTree("überweisen")
            .anyExecutor { _, _ -> }
            .toDefinition()

        assertEquals("überweisen", definition.name)
    }

    @Test
    fun `a label with whitespace stays rejected`() {
        assertThrows(CommandValidationException::class.java) {
            CommandAPICommand("über weisen").anyExecutor { _, _ -> }.toDefinition()
        }
    }

    @Test
    fun `a label with punctuation stays rejected`() {
        assertThrows(CommandValidationException::class.java) {
            CommandAPICommand("pay!").anyExecutor { _, _ -> }.toDefinition()
        }

        assertThrows(CommandValidationException::class.java) {
            CommandAPICommand("pay").withAliases("pay:now").anyExecutor { _, _ -> }.toDefinition()
        }
    }

    @Test
    fun `case insensitive duplicates stay rejected regardless of script`() {
        assertThrows(CommandValidationException::class.java) {
            CommandAPICommand("Überweisen")
                .withAliases("überweisen")
                .anyExecutor { _, _ -> }
                .toDefinition()
        }
    }
}
