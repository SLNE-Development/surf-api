package dev.slne.surf.api.minestom.server.impl.command.parser

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import dev.slne.surf.api.minestom.command.argument.ResourceArgument
import dev.slne.surf.api.minestom.command.argument.SoundArgument
import net.kyori.adventure.key.Key
import net.minestom.server.registry.DynamicRegistry
import net.minestom.server.sound.SoundEvent
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * `RegistryParser` is internal to the api module, so every case here reaches it through
 * [dev.slne.surf.api.minestom.command.argument.Argument.toDefinition]'s
 * publicly-typed `rawType`, rather than importing the parser directly.
 */
@EnvTest
class RegistryParserTest {
    private val registry = DynamicRegistry.fromMap<String>(
        Key.key("test:registry-parser"),
        java.util.Map.entry(Key.key("test:alpha"), "alpha"),
        java.util.Map.entry(Key.key("test:beta"), "beta"),
    )

    @Test
    fun `a known key resolves to its value`(env: Env) {
        val parser = SoundArgument("sound").toDefinition().rawType
        assertEquals(
            SoundEvent.fromKey("minecraft:entity.pig.ambient"),
            parser.parse(StringReader("minecraft:entity.pig.ambient")),
        )
    }

    @Test
    fun `an unknown key is rejected at the start of the key`(env: Env) {
        val parser = SoundArgument("sound").toDefinition().rawType

        val failure = assertThrows(CommandSyntaxException::class.java) {
            parser.parse(StringReader("minecraft:not_a_real_sound"))
        }
        assertEquals(0, failure.cursor)
    }

    @Test
    fun `suggestions are limited to the keys that continue what was typed`(env: Env) {
        val parser = ResourceArgument("value", "test:registry-parser", registry)
            .toDefinition()
            .rawType
        val context = CommandDispatcher<Any>().parse("", Any()).context.build("")

        val suggestions = parser.listSuggestions(context, SuggestionsBuilder("test:al", 0)).join()

        assertEquals(listOf("test:alpha"), suggestions.list.map { it.text })
    }
}
