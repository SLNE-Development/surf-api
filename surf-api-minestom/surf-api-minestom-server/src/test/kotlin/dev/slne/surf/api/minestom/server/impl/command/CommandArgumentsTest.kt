package dev.slne.surf.api.minestom.server.impl.command

import dev.slne.surf.api.minestom.command.exception.CommandSyntaxException
import dev.slne.surf.api.minestom.command.exception.CommandValidationException
import dev.slne.surf.api.minestom.command.exception.WrapperCommandSyntaxException
import dev.slne.surf.api.minestom.command.executor.CommandArguments
import dev.slne.surf.api.minestom.command.executor.ParsedArgument
import net.kyori.adventure.text.Component
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CommandArgumentsTest {
    @Test
    fun `resolves values explicitly and by property delegate`() {
        val arguments = CommandArguments.of(
            listOf(
                ParsedArgument("target", "Deniz", "Deniz", present = true),
                ParsedArgument("reason", null, null, present = false),
            ),
        )

        val target: String by arguments
        val reason: String? by arguments

        assertEquals("Deniz", arguments.get<String>("target"))
        assertEquals("Deniz", target)
        assertNull(reason)
        assertEquals("Deniz", arguments.getRaw("target"))
        assertThrows<IllegalArgumentException> { arguments.get<Int>("target") }
    }

    @Test
    fun `keeps parsed arguments ordered and distinguishes absent values`() {
        val supplied = mutableListOf(
            ParsedArgument("target", "Deniz", "Deniz", present = true),
            ParsedArgument("limit", 3, "3", present = false),
            ParsedArgument("reason", null, null, present = false),
        )
        val arguments = CommandArguments.of(supplied)
        supplied.clear()

        assertEquals(3, arguments.getOptional<Int>("limit"))
        assertEquals("Deniz", arguments[0])
        assertNull(arguments[2])
        assertEquals("3", arguments.getRaw(1))
        assertNull(arguments.getRaw(4))
        assertEquals(listOf("Deniz"), arguments.rawArguments())
        assertTrue("target" in arguments)
        assertFalse("limit" in arguments)
        assertFalse("missing" in arguments)
        assertThrows<IllegalArgumentException> { arguments.get<String>("reason") }
        assertThrows<IllegalArgumentException> { arguments.get<String>("missing") }
        assertThrows<IllegalArgumentException> {
            CommandArguments.of(listOf(ParsedArgument("target", "one", "one", true), ParsedArgument("target", "two", "two", true)))
        }
    }

    @Test
    fun `preserves syntax failure context when wrapped`() {
        val component = Component.text("Expected a destination")
        val cause = IllegalStateException("bad input")
        val syntax = CommandSyntaxException(component, "/warp", 5, cause)
        val wrapper = WrapperCommandSyntaxException(syntax)

        assertEquals("Expected a destination", syntax.message)
        assertSame(component, wrapper.component)
        assertEquals("/warp", wrapper.input)
        assertEquals(5, wrapper.cursor)
        assertSame(syntax, wrapper.exception)
        assertSame(syntax, wrapper.cause)
        assertEquals("invalid", CommandValidationException("invalid").message)
    }
}
