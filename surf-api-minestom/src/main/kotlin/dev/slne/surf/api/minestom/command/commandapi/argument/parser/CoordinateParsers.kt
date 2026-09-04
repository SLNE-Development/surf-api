package dev.slne.surf.api.minestom.command.commandapi.argument.parser

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import dev.slne.surf.api.minestom.command.commandapi.argument.Axis
import dev.slne.surf.api.minestom.command.commandapi.argument.Rotation
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
 * Reads one world-coordinate component: an optional `~` marking it relative to the sender,
 * followed by an optional numeric offset that defaults to `0` when omitted. A leading `^` is
 * rejected here, since a position's components must agree on whether they are local.
 *
 * Absolute (non-`~`) values are read as whole numbers when [blockAligned] is set; a `~` offset
 * always accepts a decimal regardless of [blockAligned].
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

/** Reads one `^`-prefixed local coordinate component; any other leading character is a mixed type. */
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
 * Rebuilds this exception at [start], since [SimpleCommandExceptionType.createWithContext] bakes
 * the cursor in at construction time.
 */
private fun CommandSyntaxException.resetTo(reader: StringReader, start: Int): CommandSyntaxException {
    reader.cursor = start
    return CommandSyntaxException(type, rawMessage, reader.string, start)
}

/**
 * Returns [relative] unchanged when [sender] is present or when none of its components are
 * relative or local; otherwise rejects it, since resolving a `~` or `^` component without a sender
 * would silently substitute the origin for the sender's actual position or view.
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
 * Reads a vanilla-style position: three components (or two, when [twoDimensional], leaving the
 * middle component fixed at a non-relative `0`), each absolute, `~`-relative to the sender, or,
 * for the three-component form only, `^`-relative to the sender's facing direction. All components
 * must agree on whether they are local; mixing `^` with a bare or `~`-prefixed component is
 * rejected, and the reader's cursor is reset to the position's start before the failure is thrown.
 *
 * [blockAligned] restricts absolute (non-`~`, non-`^`) components to whole numbers, matching a
 * block position's grammar.
 *
 * The result is a Minestom [RelativeVec] carrying the parsed values and their relativity, ready to
 * be resolved against a sender with [RelativeVec.fromSender] or [RelativeVec.from].
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
 * Reads a vanilla-style rotation: a yaw component followed by a pitch component, each absolute or
 * `~`-relative to the sender's current view. Local (`^`) coordinates are not part of vanilla's
 * rotation grammar, so a leading `^` is rejected the same way it is for a position.
 *
 * The result is a [RelativeVec] whose `x` holds the yaw value and `z` the pitch value, resolved
 * against a sender's view with [RelativeVec.fromView].
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
 * Reads a swizzle of world axes, such as `xyz` or `xz`, case-insensitively and without a repeated
 * letter. An unknown letter or a repeat is rejected at the swizzle's start.
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
 * Adapts [PositionParser] to the plain [Vec] that
 * [dev.slne.surf.api.minestom.command.commandapi.argument.PositionArgument] and its 2D and block
 * variants expose, resolving relative and local coordinates against the command source.
 *
 * A relative or local coordinate requires a command source to resolve against; parsed without one
 * (as happens on the bare, sourceless overload, or when the source is not a [CommandSender]), it is
 * rejected rather than silently resolved against the origin.
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
 * Adapts [RotationParser] to the plain [Rotation] that
 * [dev.slne.surf.api.minestom.command.commandapi.argument.RotationArgument] exposes, resolving a
 * relative yaw or pitch against the command source's current view.
 *
 * A relative yaw or pitch requires a command source to resolve against; parsed without one (as
 * happens on the bare, sourceless overload, or when the source is not a [CommandSender]), it is
 * rejected rather than silently resolved against the origin's view.
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
