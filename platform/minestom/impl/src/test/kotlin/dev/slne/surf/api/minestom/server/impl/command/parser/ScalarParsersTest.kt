package dev.slne.surf.api.minestom.server.impl.command.parser

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import dev.slne.surf.api.minestom.command.argument.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.*

/**
 * Tests the angle, float range, integer range, time and UUID parsers, which are `internal` to the
 * api module, through the `rawType` of
 * [dev.slne.surf.api.minestom.command.argument.Argument.toDefinition].
 */
class ScalarParsersTest {
    @Test
    fun `uuid is read in its dashed form`() {
        val uuid = UUID.fromString("79d40754-7772-4ab4-a7c6-f21df121db4a")
        val parser = UUIDArgument("id").toDefinition().rawType
        assertEquals(uuid, parser.parse(StringReader(uuid.toString())))
    }

    @Test
    fun `a uuid with a group wider than canonical is rejected at its start`() {
        val parser = UUIDArgument("id").toDefinition().rawType
        listOf(
            "1ffffffff-0-0-0-0",
            "79d40754-7772-4ab4-a7c6-f21df121db4a0",
            "79d40754-77720-4ab4-a7c6-f21df121db4a",
        ).forEach { input ->
            val failure = assertThrows(CommandSyntaxException::class.java) { parser.parse(StringReader(input)) }
            assertEquals(0, failure.cursor, input)
        }
        assertEquals(
            UUID.fromString("79d40754-7772-4ab4-a7c6-f21df121db4a"),
            parser.parse(StringReader("79D40754-7772-4AB4-A7C6-F21DF121DB4A")),
        )
    }

    @Test
    fun `a uuid with groups shorter than canonical is accepted`() {
        val parser = UUIDArgument("id").toDefinition().rawType
        assertEquals(UUID.fromString("1234-5-6-7-8"), parser.parse(StringReader("1234-5-6-7-8")))
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
    fun `time rounds to the nearest tick like vanilla`() {
        val parser = TimeArgument("delay").toDefinition().rawType
        assertEquals(2L, parser.parse(StringReader("1.5t")).toMillis() / 50)
        assertEquals(1L, parser.parse(StringReader("1.4t")).toMillis() / 50)
        assertEquals(30L, parser.parse(StringReader("1.5s")).toMillis() / 50)
    }

    @Test
    fun `time rejects values whose duration overflows at the value's start`() {
        val parser = TimeArgument("delay").toDefinition().rawType
        listOf("99999999999999999999999999999999999999d", "99999999999999999999", "100000000000000000d").forEach { input ->
            val failure = assertThrows(CommandSyntaxException::class.java) { parser.parse(StringReader(input)) }
            assertEquals(0, failure.cursor, input)
        }
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
