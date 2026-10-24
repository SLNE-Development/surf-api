package dev.slne.surf.api.minestom.command.argument.parser

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import dev.slne.surf.api.minestom.command.argument.Axis
import dev.slne.surf.api.minestom.command.argument.Rotation
import net.minestom.server.command.CommandSender
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.utils.location.RelativeVec

private val ERROR_MIXED_TYPE = SimpleCommandExceptionType(
    LiteralMessage("Cannot mix world and local coordinates in a single position"),
)
private val ERROR_EXPECTED_VALUE = SimpleCommandExceptionType(LiteralMessage("Expected a coordinate value"))
private val ERROR_INCOMPLETE = SimpleCommandExceptionType(LiteralMessage("Incomplete coordinate"))
private val ERROR_INVALID_SWIZZLE = SimpleCommandExceptionType(LiteralMessage("Invalid swizzle"))
private val ERROR_REQUIRES_SENDER = SimpleCommandExceptionType(
    LiteralMessage("A relative (~) or local (^) coordinate requires a command source to resolve"),
)

private data class Component(val relative: Boolean, val value: Double)

/**
 * Reads one world coordinate: an absolute number, or `~` with an optional offset that defaults
 * to `0`. A leading `^` is rejected as a mix of world and local coordinates.
 *
 * With [blockAligned], absolute values must be whole numbers; `~` offsets may always be decimal.
 */
private fun readWorldComponent(reader: StringReader, blockAligned: Boolean): Component {
    if (reader.canRead() && reader.peek() == '^') throw ERROR_MIXED_TYPE.createWithContext(reader)
    if (!reader.canRead()) throw ERROR_EXPECTED_VALUE.createWithContext(reader)

    val relative = reader.peek() == '~'
    if (relative) reader.skip()

    val hasNumber = reader.canRead() && reader.peek() != ' '
    val value = when {
        !hasNumber -> 0.0
        blockAligned && !relative -> reader.readInt().toDouble()
        else -> reader.readDouble()
    }
    return Component(relative, value)
}

/**
 * Reads one `^`-prefixed local coordinate; any other leading character is rejected as a mix of
 * world and local coordinates.
 */
private fun readLocalComponent(reader: StringReader): Double {
    if (!reader.canRead()) throw ERROR_EXPECTED_VALUE.createWithContext(reader)
    if (reader.peek() != '^') throw ERROR_MIXED_TYPE.createWithContext(reader)
    reader.skip()
    return if (reader.canRead() && reader.peek() != ' ') reader.readDouble() else 0.0
}

private fun expectSeparator(reader: StringReader) {
    if (reader.canRead() && reader.peek() == ' ') {
        reader.skip()
    } else {
        throw ERROR_INCOMPLETE.createWithContext(reader)
    }
}

private fun coordinateTypeOf(vararg relative: Boolean): RelativeVec.CoordinateType =
    if (relative.any { it }) RelativeVec.CoordinateType.RELATIVE else RelativeVec.CoordinateType.ABSOLUTE

/**
 * Rebuilds this exception with its cursor at [start] and moves [reader] there, because
 * [SimpleCommandExceptionType.createWithContext] fixes the cursor at construction.
 */
private fun CommandSyntaxException.resetTo(reader: StringReader, start: Int): CommandSyntaxException {
    reader.cursor = start
    return CommandSyntaxException(type, rawMessage, reader.string, start)
}

/**
 * Returns [relative] unless [sender] is `null` and a component is relative or local.
 *
 * Resolving such a component without a sender would substitute the origin, so it is rejected
 * instead, with [reader] reset to [start].
 */
private fun requireResolvable(
    relative: RelativeVec,
    sender: CommandSender?,
    reader: StringReader,
    start: Int,
): RelativeVec {
    if (sender == null && (relative.relativeX() || relative.relativeY() || relative.relativeZ())) {
        reader.cursor = start
        throw ERROR_REQUIRES_SENDER.createWithContext(reader)
    }
    return relative
}

/**
 * Parses a vanilla position into an unresolved [RelativeVec].
 *
 * A position has three components, or `x` and `z` only when [twoDimensional]; the 2D form leaves
 * `y` as an absolute `0`. Each component is absolute or `~`-relative. The three-component form
 * alternatively accepts `^` local coordinates (left, up, forward) for all components; mixing local
 * with other components is rejected. With [blockAligned], absolute components must be whole
 * numbers.
 *
 * Failures reset the reader to the start of the position. Resolve the result with
 * [RelativeVec.fromSender] or [RelativeVec.from].
 */
internal class PositionParser(
    private val blockAligned: Boolean,
    private val twoDimensional: Boolean,
) : ArgumentType<RelativeVec> {
    override fun parse(reader: StringReader): RelativeVec {
        val start = reader.cursor
        try {
            return if (!twoDimensional && reader.canRead() && reader.peek() == '^') {
                parseLocal(reader)
            } else if (twoDimensional) {
                parseWorld2D(reader)
            } else {
                parseWorld3D(reader)
            }
        } catch (failure: CommandSyntaxException) {
            throw failure.resetTo(reader, start)
        }
    }

    private fun parseWorld3D(reader: StringReader): RelativeVec {
        val x = readWorldComponent(reader, blockAligned)
        expectSeparator(reader)
        val y = readWorldComponent(reader, blockAligned)
        expectSeparator(reader)
        val z = readWorldComponent(reader, blockAligned)
        return RelativeVec(
            Vec(x.value, y.value, z.value),
            coordinateTypeOf(x.relative, y.relative, z.relative),
            x.relative,
            y.relative,
            z.relative,
        )
    }

    private fun parseWorld2D(reader: StringReader): RelativeVec {
        val x = readWorldComponent(reader, blockAligned)
        expectSeparator(reader)
        val z = readWorldComponent(reader, blockAligned)
        return RelativeVec(
            Vec(x.value, 0.0, z.value),
            coordinateTypeOf(x.relative, z.relative),
            x.relative,
            false,
            z.relative,
        )
    }

    private fun parseLocal(reader: StringReader): RelativeVec {
        val left = readLocalComponent(reader)
        expectSeparator(reader)
        val up = readLocalComponent(reader)
        expectSeparator(reader)
        val forward = readLocalComponent(reader)
        return RelativeVec(Vec(left, up, forward), RelativeVec.CoordinateType.LOCAL, true, true, true)
    }
}

/**
 * Parses a vanilla rotation, a yaw followed by a pitch, each absolute or `~`-relative to the
 * sender's view. Local (`^`) components are rejected.
 *
 * The result stores the yaw in `x` and the pitch in `z`; resolve it with [RelativeVec.fromView].
 */
internal object RotationParser : ArgumentType<RelativeVec> {
    override fun parse(reader: StringReader): RelativeVec {
        val start = reader.cursor
        try {
            val yaw = readWorldComponent(reader, blockAligned = false)
            expectSeparator(reader)
            val pitch = readWorldComponent(reader, blockAligned = false)
            return RelativeVec(
                Vec(yaw.value, 0.0, pitch.value),
                coordinateTypeOf(yaw.relative, pitch.relative),
                yaw.relative,
                false,
                pitch.relative,
            )
        } catch (failure: CommandSyntaxException) {
            throw failure.resetTo(reader, start)
        }
    }
}

/**
 * Parses a swizzle of world axes such as `xyz` or `xz`, case-insensitively.
 *
 * An unknown or repeated letter fails at the start of the swizzle. The returned set keeps the
 * order the axes were written in.
 */
internal object AxisParser : ArgumentType<Set<Axis>> {
    override fun parse(reader: StringReader): Set<Axis> {
        val start = reader.cursor
        val axes = LinkedHashSet<Axis>()
        while (reader.canRead() && reader.peek() != ' ') {
            val axis = when (reader.read().lowercaseChar()) {
                'x' -> Axis.X
                'y' -> Axis.Y
                'z' -> Axis.Z
                else -> {
                    reader.cursor = start
                    throw ERROR_INVALID_SWIZZLE.createWithContext(reader)
                }
            }
            if (!axes.add(axis)) {
                reader.cursor = start
                throw ERROR_INVALID_SWIZZLE.createWithContext(reader)
            }
        }
        return axes
    }
}

/**
 * Adapts [PositionParser] to the resolved [Vec] exposed by
 * [dev.slne.surf.api.minestom.command.argument.PositionArgument] and its 2D and block variants.
 *
 * Relative and local coordinates are resolved against the command source. Without a
 * [CommandSender] source they are rejected.
 */
internal class PositionArgumentType(
    blockAligned: Boolean,
    twoDimensional: Boolean,
) : ArgumentType<Vec> {
    private val delegate = PositionParser(blockAligned, twoDimensional)

    override fun parse(reader: StringReader): Vec = resolve(reader, null)

    override fun <S> parse(reader: StringReader, source: S): Vec = resolve(reader, source as? CommandSender)

    private fun resolve(reader: StringReader, sender: CommandSender?): Vec {
        val start = reader.cursor
        val relative = delegate.parse(reader)
        return requireResolvable(relative, sender, reader, start).fromSender(sender)
    }
}

/**
 * Adapts [RotationParser] to the resolved [Rotation] exposed by
 * [dev.slne.surf.api.minestom.command.argument.RotationArgument].
 *
 * A relative yaw or pitch is resolved against the view of the command source, which is the origin
 * view for a source that is not an [Entity]. Without a [CommandSender] source, relative values are
 * rejected.
 */
internal object RotationArgumentType : ArgumentType<Rotation> {
    override fun parse(reader: StringReader): Rotation = resolve(reader, null)

    override fun <S> parse(reader: StringReader, source: S): Rotation = resolve(reader, source as? CommandSender)

    private fun resolve(reader: StringReader, sender: CommandSender?): Rotation {
        val start = reader.cursor
        val relative = RotationParser.parse(reader)
        val checked = requireResolvable(relative, sender, reader, start)
        val view = checked.fromView((sender as? Entity)?.position ?: Pos.ZERO)
        return Rotation(view.x().toFloat(), view.z().toFloat())
    }
}
