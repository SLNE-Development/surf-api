package dev.slne.surf.api.minestom.server.impl.command.parser

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import dev.slne.surf.api.minestom.command.argument.ParticleArgument
import net.minestom.server.particle.Particle
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * `ParticleParser` is internal to the api module, so every case here reaches it through
 * [dev.slne.surf.api.minestom.command.argument.Argument.toDefinition]'s
 * publicly-typed `rawType`, rather than importing the parser directly.
 */
@EnvTest
class ParticleParserTest {
    private val parser = ParticleArgument("particle").toDefinition().rawType

    @Test
    fun `a simple particle resolves to its value`(env: Env) {
        assertEquals(
            Particle.fromKey("minecraft:flame"),
            parser.parse(StringReader("minecraft:flame")),
        )
    }

    @Test
    fun `an option-carrying particle is rejected as unsupported at the start of the key`(env: Env) {
        val failure = assertThrows(CommandSyntaxException::class.java) {
            parser.parse(StringReader("minecraft:dust"))
        }
        assertEquals(0, failure.cursor)
    }

    @Test
    fun `an unknown key is rejected at the start of the key`(env: Env) {
        val failure = assertThrows(CommandSyntaxException::class.java) {
            parser.parse(StringReader("minecraft:not_a_real_particle"))
        }
        assertEquals(0, failure.cursor)
    }
}
