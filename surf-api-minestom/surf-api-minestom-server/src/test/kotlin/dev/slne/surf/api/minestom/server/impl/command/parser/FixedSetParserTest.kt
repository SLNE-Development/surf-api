package dev.slne.surf.api.minestom.server.impl.command.parser

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import dev.slne.surf.api.minestom.command.argument.EnumArgument
import dev.slne.surf.api.minestom.command.argument.GameModeArgument
import dev.slne.surf.api.minestom.command.argument.MultiLiteralArgument
import dev.slne.surf.api.minestom.command.argument.TeamColorArgument
import net.minestom.server.color.TeamColor
import net.minestom.server.entity.GameMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * `FixedSetParser` is internal to the api module, so every case here reaches it through
 * [dev.slne.surf.api.minestom.command.argument.Argument.toDefinition]'s
 * publicly-typed `rawType`, rather than importing the parser directly.
 */
class FixedSetParserTest {
    private enum class AccessLevel { STAFF, GUEST }

    @Test
    fun `a known value resolves`() {
        val parser = MultiLiteralArgument("side", "left", "right").toDefinition().rawType
        assertEquals("right", parser.parse(StringReader("right")))
    }

    @Test
    fun `an unknown value is rejected at its own start`() {
        val parser = MultiLiteralArgument("side", "left", "right").toDefinition().rawType
        val failure = assertThrows(CommandSyntaxException::class.java) { parser.parse(StringReader("up")) }
        assertEquals(0, failure.cursor)
    }

    @Test
    fun `the match is case sensitive and stops at whitespace`() {
        val parser = MultiLiteralArgument("side", "left", "right").toDefinition().rawType
        val reader = StringReader("left over")
        assertEquals("left", parser.parse(reader))
        assertEquals(" over", reader.remaining)

        assertThrows(CommandSyntaxException::class.java) { parser.parse(StringReader("LEFT")) }
    }

    @Test
    fun `enum argument resolves the formatted spelling to its constant`() {
        val parser = EnumArgument(
            "access",
            AccessLevel.entries,
        ) { value -> "mode-${value.name.lowercase()}" }.toDefinition().rawType

        assertEquals(AccessLevel.STAFF, parser.parse(StringReader("mode-staff")))
        assertThrows(CommandSyntaxException::class.java) { parser.parse(StringReader("staff")) }
    }

    @Test
    fun `game mode keeps its vanilla spellings and matches case sensitively`() {
        val parser = GameModeArgument("mode").toDefinition().rawType

        assertEquals(GameMode.SURVIVAL, parser.parse(StringReader("survival")))
        assertEquals(GameMode.CREATIVE, parser.parse(StringReader("creative")))
        assertEquals(GameMode.ADVENTURE, parser.parse(StringReader("adventure")))
        assertEquals(GameMode.SPECTATOR, parser.parse(StringReader("spectator")))
        assertThrows(CommandSyntaxException::class.java) { parser.parse(StringReader("SURVIVAL")) }
    }

    @Test
    fun `team color keeps its vanilla spellings and matches case sensitively`() {
        val parser = TeamColorArgument("color").toDefinition().rawType

        assertEquals(TeamColor.RED, parser.parse(StringReader("red")))
        assertEquals(TeamColor.DARK_BLUE, parser.parse(StringReader("dark_blue")))
        assertThrows(CommandSyntaxException::class.java) { parser.parse(StringReader("RED")) }
    }

    @Test
    fun `suggestions are offered case-insensitively`() {
        val parser = MultiLiteralArgument("side", "left", "right").toDefinition().rawType
        val context = CommandDispatcher<Any>().parse("", Any()).context.build("")

        val suggestions = parser.listSuggestions(context, SuggestionsBuilder("LEF", 0)).join()

        assertEquals(listOf("left"), suggestions.list.map { it.text })
    }
}
