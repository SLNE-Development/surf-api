package dev.slne.surf.api.minestom.command.argument.parser

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Entity
import net.minestom.server.utils.Range
import java.time.Duration
import java.util.*

/**
 * Parses a UUID in dashed hex form, e.g. `550e8400-e29b-41d4-a716-446655440000`.
 *
 * Groups shorter than the canonical width are accepted as well; wider groups are rejected instead
 * of being truncated.
 */
internal object UuidParser : ArgumentType<UUID> {
    private val INVALID = SimpleCommandExceptionType(LiteralMessage("Invalid UUID"))

    override fun parse(reader: StringReader): UUID {
        val start = reader.cursor
        while (reader.canRead() && isAllowed(reader.peek())) {
            reader.skip()
        }
        val text = reader.string.substring(start, reader.cursor)

        return parseDashedUuid(text) ?: run {
            reader.cursor = start
            throw INVALID.createWithContext(reader)
        }
    }

    private fun isAllowed(character: Char): Boolean = character in '0'..'9' ||
            character in 'a'..'f' || character in 'A'..'F' || character == '-'
}

/**
 * Parses [text] as a UUID of five dashed hex groups, or returns `null` for any other text.
 *
 * As with [UUID.fromString], a group may be shorter than its canonical 8-4-4-4-12 width; a wider
 * group is rejected instead of being truncated. Ordinary names are rejected without an exception.
 */
internal fun parseDashedUuid(text: String): UUID? {
    var group = 0
    var groupLength = 0
    for (character in text) {
        if (character == '-') {
            if (groupLength == 0 || ++group >= UUID_GROUP_WIDTHS.size) return null
            groupLength = 0
            continue
        }
        val hex = character in '0'..'9' || character in 'a'..'f' || character in 'A'..'F'
        if (!hex || ++groupLength > UUID_GROUP_WIDTHS[group]) return null
    }
    if (group != UUID_GROUP_WIDTHS.lastIndex || groupLength == 0) return null
    return UUID.fromString(text)
}

private val UUID_GROUP_WIDTHS = intArrayOf(8, 4, 4, 4, 12)

/**
 * Wraps [degrees] into `[-180, 180)`.
 */
internal fun wrapDegrees(degrees: Float): Float {
    var wrapped = degrees % 360f
    if (wrapped >= 180f) wrapped -= 360f
    if (wrapped < -180f) wrapped += 360f
    return wrapped
}

/**
 * Parses an angle in degrees, either absolute or `~` with an optional offset relative to the
 * source's yaw. The result is wrapped into `[-180, 180)`.
 *
 * A source that is not an [Entity], such as the console, has yaw `0`.
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
}

/**
 * Parses a vanilla time value: a number with an optional unit of `d` (24000 ticks), `s` (20 ticks)
 * or `t` (ticks). Without a unit the number counts ticks.
 *
 * The value is rounded to the nearest tick and converted to a [Duration] using
 * [MinecraftServer.TICK_MS]. Negative values, values whose duration does not fit a [Duration] in
 * milliseconds and other units are rejected.
 */
internal object TimeParser : ArgumentType<Duration> {
    private val INVALID_UNIT = DynamicCommandExceptionType { unit ->
        LiteralMessage("Invalid unit '$unit'; expected 'd', 's', 't' or none")
    }
    private val NEGATIVE = SimpleCommandExceptionType(LiteralMessage("Duration must not be negative"))
    private val OUT_OF_RANGE = SimpleCommandExceptionType(LiteralMessage("Duration is out of range"))
    private val MAX_TICKS = Long.MAX_VALUE / MinecraftServer.TICK_MS

    override fun parse(reader: StringReader): Duration {
        val start = reader.cursor
        val amount = reader.readFloat()
        val unit = if (reader.canRead() && reader.peek() != ' ') reader.read() else null

        val factor = unitFactor(unit) ?: run {
            reader.cursor = start
            throw INVALID_UNIT.createWithContext(reader, unit.toString())
        }

        val scaled = amount * factor
        if (!scaled.isFinite()) {
            reader.cursor = start
            throw OUT_OF_RANGE.createWithContext(reader)
        }

        val ticks = Math.round(scaled.toDouble())
        if (ticks < 0) {
            reader.cursor = start
            throw NEGATIVE.createWithContext(reader)
        }
        if (ticks > MAX_TICKS) {
            reader.cursor = start
            throw OUT_OF_RANGE.createWithContext(reader)
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
 * Parses an integer range in vanilla's `min..max` syntax.
 *
 * An omitted bound defaults to [Int.MIN_VALUE] or [Int.MAX_VALUE]. A bare value denotes a range
 * containing only that value. At least one bound is required, and the minimum must not exceed the
 * maximum.
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
 * Parses a float range in vanilla's `min..max` syntax.
 *
 * An omitted bound defaults to `-Float.MAX_VALUE` or `Float.MAX_VALUE`. A bare value denotes a
 * range containing only that value. At least one bound is required, and the minimum must not
 * exceed the maximum.
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
 * Reads a `lower..upper` range, converting each bound with [parse]. A bare value supplies both
 * bounds; an empty bound is returned as `null`.
 *
 * The cursor of [reader] is reset to the start of the range before an exception is thrown.
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
 * Reads one range bound: an optional `-`, digits and an optional decimal part.
 *
 * A `..` separator ends the bound and is not read as a decimal point.
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
