package dev.slne.surf.api.minestom.command.commandapi.argument.parser

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Entity
import net.minestom.server.utils.Range
import java.time.Duration
import java.util.UUID

/**
 * Reads a UUID in its canonical dashed hex form, e.g. `550e8400-e29b-41d4-a716-446655440000`.
 */
internal object UuidParser : ArgumentType<UUID> {
    private val INVALID = SimpleCommandExceptionType(LiteralMessage("Invalid UUID"))

    override fun parse(reader: StringReader): UUID {
        val start = reader.cursor
        while (reader.canRead() && isAllowed(reader.peek())) {
            reader.skip()
        }
        val text = reader.string.substring(start, reader.cursor)

        return try {
            UUID.fromString(text)
        } catch (cause: IllegalArgumentException) {
            reader.cursor = start
            throw INVALID.createWithContext(reader)
        }
    }

    private fun isAllowed(character: Char): Boolean = character in '0'..'9' ||
            character in 'a'..'f' || character in 'A'..'F' || character == '-'
}

/**
 * Reads an angle in degrees: an absolute value, or `~` optionally followed by an offset applied to
 * the source's own yaw. The result is wrapped into `[-180, 180)`.
 *
 * A source that has no position - the console, for instance - counts as facing yaw `0`.
 */
internal object AngleParser : ArgumentType<Float> {
    private val INCOMPLETE = SimpleCommandExceptionType(LiteralMessage("Expected an angle"))
    private val INVALID = SimpleCommandExceptionType(LiteralMessage("Invalid angle"))

    override fun parse(reader: StringReader): Float = read(reader, yaw = 0f)

    override fun <S> parse(reader: StringReader, source: S): Float =
        read(reader, (source as? Entity)?.position?.yaw ?: 0f)

    private fun read(reader: StringReader, yaw: Float): Float {
        if (!reader.canRead()) throw INCOMPLETE.createWithContext(reader)

        val relative = reader.peek() == '~'
        if (relative) reader.skip()

        val start = reader.cursor
        val value = if (reader.canRead() && reader.peek() != ' ') reader.readFloat() else 0f
        if (!value.isFinite()) {
            reader.cursor = start
            throw INVALID.createWithContext(reader)
        }

        return wrapDegrees(if (relative) value + yaw else value)
    }

    private fun wrapDegrees(degrees: Float): Float {
        var wrapped = degrees % 360f
        if (wrapped >= 180f) wrapped -= 360f
        if (wrapped < -180f) wrapped += 360f
        return wrapped
    }
}

/**
 * Reads a suffixed vanilla duration: a number followed by an optional unit, `d` for days
 * (24000 ticks), `s` for seconds (20 ticks) or `t` for ticks; an absent suffix also means ticks.
 *
 * The result is rejected if it would be negative or if the suffix is anything else.
 */
internal object TimeParser : ArgumentType<Duration> {
    private val INVALID_UNIT = DynamicCommandExceptionType { unit ->
        LiteralMessage("Invalid unit '$unit'; expected 'd', 's', 't' or none")
    }
    private val NEGATIVE = SimpleCommandExceptionType(LiteralMessage("Duration must not be negative"))

    override fun parse(reader: StringReader): Duration {
        val start = reader.cursor
        val amount = reader.readFloat()
        val unit = if (reader.canRead() && reader.peek() != ' ') reader.read() else null

        val factor = unitFactor(unit) ?: run {
            reader.cursor = start
            throw INVALID_UNIT.createWithContext(reader, unit.toString())
        }

        val ticks = (amount * factor).toLong()
        if (ticks < 0) {
            reader.cursor = start
            throw NEGATIVE.createWithContext(reader)
        }

        return Duration.ofMillis(ticks * MinecraftServer.TICK_MS)
    }

    private fun unitFactor(unit: Char?): Int? = when (unit) {
        null, 't' -> 1
        's' -> 20
        'd' -> 24000
        else -> null
    }
}

/**
 * Reads an integer range in vanilla's `min..max` syntax.
 *
 * Either bound may be omitted, in which case it resolves to [Int.MIN_VALUE] or [Int.MAX_VALUE]
 * respectively. A bare value is shorthand for a range containing only that value. At least one
 * bound must be present, and the lower bound must not exceed the upper one.
 */
internal object IntegerRangeParser : ArgumentType<Range.Int> {
    override fun parse(reader: StringReader): Range.Int {
        val start = reader.cursor
        val (min, max) = readRange(reader, String::toIntOrNull)
        return try {
            Range.Int(min ?: Int.MIN_VALUE, max ?: Int.MAX_VALUE)
        } catch (cause: IllegalArgumentException) {
            reader.cursor = start
            throw RANGE_INVERTED.createWithContext(reader)
        }
    }
}

/**
 * Reads a float range in vanilla's `min..max` syntax.
 *
 * Either bound may be omitted, in which case it resolves to `-Float.MAX_VALUE` or
 * `Float.MAX_VALUE` respectively. A bare value is shorthand for a range containing only that
 * value. At least one bound must be present, and the lower bound must not exceed the upper one.
 */
internal object FloatRangeParser : ArgumentType<Range.Float> {
    override fun parse(reader: StringReader): Range.Float {
        val start = reader.cursor
        val (min, max) = readRange(reader, String::toFloatOrNull)
        return try {
            Range.Float(min ?: -Float.MAX_VALUE, max ?: Float.MAX_VALUE)
        } catch (cause: IllegalArgumentException) {
            reader.cursor = start
            throw RANGE_INVERTED.createWithContext(reader)
        }
    }
}

private val RANGE_EMPTY = SimpleCommandExceptionType(LiteralMessage("Expected value or range of values"))
private val RANGE_INVALID = DynamicCommandExceptionType { value -> LiteralMessage("Invalid range value '$value'") }
private val RANGE_INVERTED = SimpleCommandExceptionType(LiteralMessage("Range minimum must not exceed its maximum"))

/**
 * Reads a `<lower>..<upper>` range (either side optional, a bare value standing for both), and
 * converts each side with [parse]. Resets [reader]'s cursor to the range's start before throwing.
 */
private fun <T> readRange(reader: StringReader, parse: (String) -> T?): Pair<T?, T?> {
    val start = reader.cursor
    val lowerText = readRangeBoundText(reader)
    val upperText = if (reader.canRead(2) && reader.peek() == '.' && reader.peek(1) == '.') {
        reader.skip()
        reader.skip()
        readRangeBoundText(reader)
    } else {
        lowerText
    }

    if (lowerText.isEmpty() && upperText.isEmpty()) {
        reader.cursor = start
        throw RANGE_EMPTY.createWithContext(reader)
    }

    fun parseBound(text: String): T? {
        if (text.isEmpty()) return null
        return parse(text) ?: run {
            reader.cursor = start
            throw RANGE_INVALID.createWithContext(reader, text)
        }
    }

    return parseBound(lowerText) to parseBound(upperText)
}

/**
 * Reads one side of a range: an optional `-`, digits, and an optional single `.` followed by
 * more digits. Stops before a `..` separator instead of consuming it as a decimal point.
 */
private fun readRangeBoundText(reader: StringReader): String {
    val start = reader.cursor
    if (reader.canRead() && reader.peek() == '-') {
        reader.skip()
    }
    while (reader.canRead() && reader.peek() in '0'..'9') {
        reader.skip()
    }
    if (reader.canRead() && reader.peek() == '.' && !(reader.canRead(2) && reader.peek(1) == '.')) {
        reader.skip()
        while (reader.canRead() && reader.peek() in '0'..'9') {
            reader.skip()
        }
    }
    return reader.string.substring(start, reader.cursor)
}
