package dev.slne.surf.api.minestom.server.impl.command.parser

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import dev.slne.surf.api.minestom.command.argument.InstanceArgument
import net.minestom.server.world.DimensionType
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * `InstanceParser` is internal to the api module, so every case here reaches it through
 * [dev.slne.surf.api.minestom.command.argument.Argument.toDefinition]'s
 * publicly-typed `rawType`, rather than importing the parser directly.
 */
@EnvTest
class InstanceParserTest {
    private val parser = InstanceArgument("instance").toDefinition().rawType

    @Test
    fun `an instance resolves by its uuid`(env: Env) {
        val instance = env.process().instance().createInstanceContainer(DimensionType.OVERWORLD)
        try {
            assertEquals(instance, parser.parse(StringReader(instance.uuid.toString())))
        } finally {
            env.destroyInstance(instance)
        }
    }

    @Test
    fun `an instance resolves by its unique dimension name`(env: Env) {
        val instance = env.process().instance().createInstanceContainer(DimensionType.THE_END)
        try {
            assertEquals(instance, parser.parse(StringReader("minecraft:the_end")))
        } finally {
            env.destroyInstance(instance)
        }
    }

    @Test
    fun `an ambiguous dimension name is rejected at the start of the token`(env: Env) {
        val first = env.process().instance().createInstanceContainer(DimensionType.THE_NETHER)
        val second = env.process().instance().createInstanceContainer(DimensionType.THE_NETHER)
        try {
            val failure = assertThrows(CommandSyntaxException::class.java) {
                parser.parse(StringReader("minecraft:the_nether"))
            }
            assertEquals(0, failure.cursor)
        } finally {
            env.destroyInstance(second)
            env.destroyInstance(first)
        }
    }

    @Test
    fun `an unknown token is rejected at its start`(env: Env) {
        val failure = assertThrows(CommandSyntaxException::class.java) {
            parser.parse(StringReader("not-a-real-instance"))
        }
        assertEquals(0, failure.cursor)
    }
}
