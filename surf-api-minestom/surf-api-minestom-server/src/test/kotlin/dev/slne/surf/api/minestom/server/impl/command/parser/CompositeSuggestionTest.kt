package dev.slne.surf.api.minestom.server.impl.command.parser

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import dev.slne.surf.api.minestom.command.argument.CustomArgument
import dev.slne.surf.api.minestom.command.argument.EnumArgument
import dev.slne.surf.api.minestom.command.argument.ListArgument
import dev.slne.surf.api.minestom.command.argument.MultiLiteralArgument
import dev.slne.surf.api.minestom.command.argument.PlayerArgument
import dev.slne.surf.api.minestom.command.argument.PlayersArgument
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * An argument that wraps another one has to hand completion through to it, or the values the inner
 * argument offers become unreachable.
 */
@EnvTest
class CompositeSuggestionTest {
    private enum class Mode { LOW, HIGH }

    private val context = CommandDispatcher<Any>().parse("", Any()).context.build("")

    private fun suggest(
        type: com.mojang.brigadier.arguments.ArgumentType<*>,
        typed: String,
    ) = type.listSuggestions(context, SuggestionsBuilder(typed, 0)).join()

    @Test
    fun `a custom argument offers what its base offers`(env: Env) {
        val parser = CustomArgument(MultiLiteralArgument("side", "left", "right")) { info ->
            info.baseValue.uppercase()
        }.toDefinition().rawType

        assertEquals(listOf("left"), suggest(parser, "le").list.map { it.text })
    }

    @Test
    fun `a list argument completes the element being typed and replaces only it`(env: Env) {
        val parser = ListArgument("modes", EnumArgument("mode", Mode.entries)).toDefinition().rawType

        val suggestions = suggest(parser, "low,HI")

        assertEquals(listOf("high"), suggestions.list.map { it.text })
        assertEquals(4, suggestions.range.start)
        assertEquals(6, suggestions.range.end)
    }

    @Test
    fun `an entity argument offers the selectors it accepts`(env: Env) {
        val single = suggest(PlayerArgument("target").toDefinition().rawType, "@")
        val multiple = suggest(PlayersArgument("targets").toDefinition().rawType, "@")

        assertEquals(setOf("@p", "@r", "@s"), single.list.map { it.text }.toSet())
        assertEquals(setOf("@a", "@p", "@r", "@s"), multiple.list.map { it.text }.toSet())
    }
}
