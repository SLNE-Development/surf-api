package dev.slne.surf.api.minestom.server.impl.command.brigadier

import dev.slne.surf.api.minestom.command.argument.BiomeArgument
import dev.slne.surf.api.minestom.command.argument.BooleanArgument
import dev.slne.surf.api.minestom.command.argument.CustomArgument
import dev.slne.surf.api.minestom.command.argument.EnumArgument
import dev.slne.surf.api.minestom.command.argument.GameModeArgument
import dev.slne.surf.api.minestom.command.argument.InstanceArgument
import dev.slne.surf.api.minestom.command.argument.IntegerArgument
import dev.slne.surf.api.minestom.command.argument.ListArgument
import dev.slne.surf.api.minestom.command.argument.MultiLiteralArgument
import dev.slne.surf.api.minestom.command.argument.PlayerArgument
import dev.slne.surf.api.minestom.command.argument.ResourceArgument
import dev.slne.surf.api.minestom.command.argument.SoundArgument
import dev.slne.surf.api.minestom.command.argument.StringArgument
import dev.slne.surf.api.minestom.command.argument.TeamColorArgument
import net.kyori.adventure.key.Key
import net.minestom.server.command.builder.arguments.minecraft.SuggestionType
import net.minestom.server.registry.DynamicRegistry
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * A client only asks the server to complete a node that says so. Every kind whose values the server
 * alone knows therefore has to be declared `ask_server`, or the completions its parser computes are
 * never requested.
 */
@EnvTest
class NodeDeclarationTest {
    private enum class Mode { LOW, HIGH }

    private val declarations = NodeDeclarations()
    private val askServer = SuggestionType.ASK_SERVER.identifier

    @Test
    fun `kinds whose values only the server knows ask the server`(env: Env) {
        val registry = DynamicRegistry.fromMap<String>(Key.key("test:declaration-samples"))

        listOf(
            SoundArgument("value"),
            BiomeArgument("value"),
            InstanceArgument("value"),
            ResourceArgument("value", "test:declaration-sample", registry),
            PlayerArgument("value"),
            MultiLiteralArgument("value", "one", "two"),
            EnumArgument("value", Mode.entries),
        ).forEach { argument ->
            val definition = argument.toDefinition()
            assertEquals(
                askServer,
                declarations.of(definition).suggestionsType,
                definition.kind.toString(),
            )
        }
    }

    @Test
    fun `a wrapping argument inherits whether its base asks the server`(env: Env) {
        val wrapped = CustomArgument(InstanceArgument("value")) { info -> info.baseValue }
        val listed = ListArgument("value", EnumArgument("mode", Mode.entries))
        val plain = CustomArgument(IntegerArgument("value")) { info -> info.baseValue * 2 }

        assertEquals(askServer, declarations.of(wrapped.toDefinition()).suggestionsType)
        assertEquals(askServer, declarations.of(listed.toDefinition()).suggestionsType)
        assertNull(declarations.of(plain.toDefinition()).suggestionsType)
    }

    @Test
    fun `kinds the client completes from its own data are left to it`(env: Env) {
        listOf(
            BooleanArgument("value"),
            IntegerArgument("value"),
            StringArgument("value"),
            GameModeArgument("value"),
            TeamColorArgument("value"),
        ).forEach { argument ->
            val definition = argument.toDefinition()
            assertNull(declarations.of(definition).suggestionsType, definition.kind.toString())
        }
    }
}
