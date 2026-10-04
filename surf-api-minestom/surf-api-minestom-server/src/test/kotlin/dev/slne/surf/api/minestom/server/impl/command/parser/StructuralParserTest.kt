package dev.slne.surf.api.minestom.server.impl.command.parser

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import dev.slne.surf.api.minestom.command.argument.CommandArgument
import dev.slne.surf.api.minestom.command.argument.CustomArgument
import dev.slne.surf.api.minestom.command.argument.IntegerArgument
import dev.slne.surf.api.minestom.command.argument.ListArgument
import dev.slne.surf.api.minestom.command.argument.PositionArgument
import dev.slne.surf.api.minestom.command.argument.StringArgument
import dev.slne.surf.api.minestom.command.exception.WrapperCommandSyntaxException
import net.kyori.adventure.identity.Identity
import net.kyori.adventure.text.Component
import net.minestom.server.command.CommandSender
import net.minestom.server.coordinate.Vec
import net.minestom.server.tag.TagHandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * The parsers under test here are `internal` to the api module, so every case reaches them through
 * [dev.slne.surf.api.minestom.command.argument.Argument.toDefinition]'s publicly-typed
 * `rawType`, rather than importing a parser directly.
 */
class StructuralParserTest {
    private val sender = StubSender()

    @Test
    fun `custom argument applies its parser to the base value`() {
        val argument = CustomArgument(StringArgument("name")) { info -> "resolved:${info.baseValue}" }
        assertEquals(
            "resolved:lobby",
            argument.toDefinition().rawType.parse(StringReader("lobby"), sender),
        )
    }

    @Test
    fun `custom argument info carries the sender and the exact consumed input`() {
        val argument = CustomArgument(StringArgument("name")) { info ->
            Triple(info.sender, info.currentInput, info.baseValue)
        }
        val reader = StringReader("lobby rest")
        val (capturedSender, currentInput, baseValue) = argument.toDefinition().rawType.parse(reader, sender)

        assertEquals(sender, capturedSender)
        assertEquals("lobby", currentInput)
        assertEquals("lobby", baseValue)
        assertEquals(" rest", reader.remaining)
    }

    @Test
    fun `custom argument shares its base argument's node name`() {
        val argument = CustomArgument(StringArgument("server")) { info -> info.baseValue.length }
        assertEquals("server", argument.nodeName)
    }

    @Test
    fun `custom argument without a command source is rejected`() {
        val argument = CustomArgument(StringArgument("name")) { info -> info.baseValue }
        val failure = assertThrows(CommandSyntaxException::class.java) {
            argument.toDefinition().rawType.parse(StringReader("lobby"))
        }
        assertEquals(0, failure.cursor)
    }

    @Test
    fun `custom argument forwards the command source into its base argument`() {
        val argument = CustomArgument(PositionArgument("pos")) { info -> info.baseValue }
        assertEquals(
            Vec(1.0, 2.0, 3.0),
            argument.toDefinition().rawType.parse(StringReader("~1 ~2 ~3"), sender),
        )
    }

    @Test
    fun `a brigadier syntax exception thrown by the lambda passes through unchanged`() {
        val original = SimpleCommandExceptionType(LiteralMessage("nope")).create()
        val argument = CustomArgument(StringArgument("name")) { throw original }
        val failure = assertThrows(CommandSyntaxException::class.java) {
            argument.toDefinition().rawType.parse(StringReader("lobby"), sender)
        }
        assertEquals(original, failure)
    }

    @Test
    fun `the project's own syntax exception is translated to a brigadier syntax error`() {
        val argument = CustomArgument(StringArgument("name")) { info ->
            throw dev.slne.surf.api.minestom.command.exception.CommandSyntaxException(
                Component.text("bad value"),
            )
        }
        val reader = StringReader("lobby")
        val failure = assertThrows(CommandSyntaxException::class.java) {
            argument.toDefinition().rawType.parse(reader, sender)
        }
        assertEquals(0, failure.cursor)
        assertEquals("bad value", failure.rawMessage.string)
    }

    @Test
    fun `a wrapped project syntax exception is unwrapped and translated`() {
        val argument = CustomArgument(StringArgument("name")) { info ->
            throw WrapperCommandSyntaxException(
                dev.slne.surf.api.minestom.command.exception.CommandSyntaxException(
                    Component.text("wrapped failure"),
                ),
            )
        }
        val reader = StringReader("lobby")
        val failure = assertThrows(CommandSyntaxException::class.java) {
            argument.toDefinition().rawType.parse(reader, sender)
        }
        assertEquals(0, failure.cursor)
        assertEquals("wrapped failure", failure.rawMessage.string)
    }

    @Test
    fun `list argument reads delimited elements`() {
        val argument = ListArgument("values", IntegerArgument("value"), ',', allowEmpty = false)
        assertEquals(listOf(1, 2, 3), argument.toDefinition().rawType.parse(StringReader("1,2,3")))
    }

    @Test
    fun `list argument skips whitespace around its delimiter`() {
        val argument = ListArgument("values", IntegerArgument("value"), ',', allowEmpty = false)
        assertEquals(listOf(1, 2, 3), argument.toDefinition().rawType.parse(StringReader(" 1 , 2 ,3 ")))
    }

    @Test
    fun `list argument rejects an empty list unless allowEmpty is set`() {
        val required = ListArgument("values", IntegerArgument("value"), ',', allowEmpty = false)
        assertThrows(CommandSyntaxException::class.java) {
            required.toDefinition().rawType.parse(StringReader(""))
        }

        val optional = ListArgument("values", IntegerArgument("value"), ',', allowEmpty = true)
        assertEquals(emptyList<Int>(), optional.toDefinition().rawType.parse(StringReader("")))
    }

    @Test
    fun `list argument rejects a stray delimiter through the element parser's own empty-input rejection`() {
        val argument = ListArgument("values", IntegerArgument("value"), ',', allowEmpty = false)
        assertThrows(CommandSyntaxException::class.java) {
            argument.toDefinition().rawType.parse(StringReader("1,,2"))
        }
    }

    @Test
    fun `list argument rejects a stray delimiter for an element type that would otherwise accept empty input`() {
        val argument = ListArgument("tags", StringArgument("tag"), ',', allowEmpty = false)

        assertThrows(CommandSyntaxException::class.java) {
            argument.toDefinition().rawType.parse(StringReader("a,,b"))
        }
        assertThrows(CommandSyntaxException::class.java) {
            argument.toDefinition().rawType.parse(StringReader("a,b,"))
        }
        assertThrows(CommandSyntaxException::class.java) {
            argument.toDefinition().rawType.parse(StringReader(",a,b"))
        }
    }

    @Test
    fun `list argument allows an element type's empty input when allowEmpty is set`() {
        val argument = ListArgument("tags", StringArgument("tag"), ',', allowEmpty = true)
        assertEquals(
            listOf("a", "", "b", ""),
            argument.toDefinition().rawType.parse(StringReader("a,,b,")),
        )
    }

    @Test
    fun `command argument reads the remaining input as a string`() {
        val argument = CommandArgument("command")
        assertEquals(
            "give @s stone 1",
            argument.toDefinition().rawType.parse(StringReader("give @s stone 1")),
        )
    }

    private class StubSender : CommandSender {
        override fun identity(): Identity = Identity.nil()
        override fun tagHandler(): TagHandler = TagHandler.newHandler()
    }
}
