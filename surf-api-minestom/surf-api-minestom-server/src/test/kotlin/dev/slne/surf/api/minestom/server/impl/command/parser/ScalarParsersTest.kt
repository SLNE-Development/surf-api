package dev.slne.surf.api.minestom.server.impl.command.parser

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import dev.slne.surf.api.minestom.command.argument.AngleArgument
import dev.slne.surf.api.minestom.command.argument.FloatRangeArgument
import dev.slne.surf.api.minestom.command.argument.IntegerRangeArgument
import dev.slne.surf.api.minestom.command.argument.TimeArgument
import dev.slne.surf.api.minestom.command.argument.UUIDArgument
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * All five parsers under test here are `internal` to the api module, so every case reaches them
 * through [dev.slne.surf.api.minestom.command.argument.Argument.toDefinition]'s
 * publicly-typed `rawType`, rather than importing the parser objects directly.
 */
class ScalarParsersTest {
    @Test
    fun `uuid is read in its dashed form`() {
        val uuid = UUID.fromString("79d40754-7772-4ab4-a7c6-f21df121db4a")
        val parser = UUIDArgument("id").toDefinition().rawType
        assertEquals(uuid, parser.parse(StringReader(uuid.toString())))
    }

    @Test
    fun `a malformed uuid is rejected at its start`() {
        val parser = UUIDArgument("id").toDefinition().rawType
        val reader = StringReader("1234-5678")
        val failure = assertThrows(CommandSyntaxException::class.java) { parser.parse(reader) }
        assertEquals(0, failure.cursor)
    }

    @Test
    fun `integer range covers both bounds, one bound and a single value`() {
        val parser = IntegerRangeArgument("levels").toDefinition().rawType
        assertEquals(1 to 4, parser.parse(StringReader("1..4")).let { it.min() to it.max() })
        assertEquals(1 to Int.MAX_VALUE, parser.parse(StringReader("1..")).let { it.min() to it.max() })
        assertEquals(Int.MIN_VALUE to 4, parser.parse(StringReader("..4")).let { it.min() to it.max() })
        assertEquals(3 to 3, parser.parse(StringReader("3")).let { it.min() to it.max() })
    }

    @Test
    fun `an integer range with neither bound present is rejected at its start`() {
        val parser = IntegerRangeArgument("levels").toDefinition().rawType
        val reader = StringReader("..")
        val failure = assertThrows(CommandSyntaxException::class.java) { parser.parse(reader) }
        assertEquals(0, failure.cursor)
    }

    @Test
    fun `float range is reachable through rawType and covers a partial bound`() {
        val parser = FloatRangeArgument("speeds").toDefinition().rawType
        val range = parser.parse(StringReader("1.5.."))
        assertEquals(1.5f, range.min())
        assertEquals(Float.MAX_VALUE, range.max())
    }

    @Test
    fun `a float range with neither bound present is rejected at its start`() {
        val parser = FloatRangeArgument("speeds").toDefinition().rawType
        val reader = StringReader("..")
        val failure = assertThrows(CommandSyntaxException::class.java) { parser.parse(reader) }
        assertEquals(0, failure.cursor)
    }

    @Test
    fun `time accepts every vanilla unit and defaults to ticks`() {
        val parser = TimeArgument("delay").toDefinition().rawType
        assertEquals(20L, parser.parse(StringReader("20")).toMillis() / 50)
        assertEquals(20L, parser.parse(StringReader("1s")).toMillis() / 50)
        assertEquals(24000L, parser.parse(StringReader("1d")).toMillis() / 50)
    }

    @Test
    fun `time rejects an unknown unit at the value's start`() {
        val parser = TimeArgument("delay").toDefinition().rawType
        val reader = StringReader("5x")
        val failure = assertThrows(CommandSyntaxException::class.java) { parser.parse(reader) }
        assertEquals(0, failure.cursor)
    }

    @Test
    fun `time rejects a negative duration at the value's start`() {
        val parser = TimeArgument("delay").toDefinition().rawType
        val reader = StringReader("-5")
        val failure = assertThrows(CommandSyntaxException::class.java) { parser.parse(reader) }
        assertEquals(0, failure.cursor)
    }

    @Test
    fun `angle rejects a bare tilde with trailing text`() {
        val parser = AngleArgument("angle").toDefinition().rawType
        assertEquals(90.0f, parser.parse(StringReader("90")))
        assertThrows(CommandSyntaxException::class.java) { parser.parse(StringReader("x")) }
    }
}
