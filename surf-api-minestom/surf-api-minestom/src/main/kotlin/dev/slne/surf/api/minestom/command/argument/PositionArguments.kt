/*
 * Substantially translated from CommandAPI 12.0.0 (https://github.com/CommandAPI/CommandAPI).
 * MIT License, Copyright (c) 2020 - 2022 Jorel Ali.
 * The complete license is distributed in META-INF/LICENSES/CommandAPI-LICENSE.txt.
 */
package dev.slne.surf.api.minestom.command.argument

import com.mojang.brigadier.arguments.ArgumentType
import dev.slne.surf.api.minestom.command.argument.parser.AngleParser
import dev.slne.surf.api.minestom.command.argument.parser.AxisParser
import dev.slne.surf.api.minestom.command.argument.parser.PositionArgumentType
import dev.slne.surf.api.minestom.command.argument.parser.RotationArgumentType
import net.minestom.server.coordinate.Vec
import kotlin.math.floor

/**
 * A world axis, as returned by [AxisArgument].
 */
enum class Axis {
    X,
    Y,
    Z,
}

/**
 * A view direction in degrees, as returned by [RotationArgument].
 */
data class Rotation(val yaw: Float, val pitch: Float)

/**
 * A position of three coordinates, each absolute or `~`-relative, or three `^` local coordinates.
 *
 * The returned [Vec] is already resolved against the sender: relative coordinates are added to the
 * sender's position and local coordinates follow its view direction. Parsing fails when a relative
 * or local coordinate cannot be resolved because there is no command source.
 */
class PositionArgument(nodeName: String) : Argument<Vec>(nodeName) {
    override val kind = ArgumentKind.Position
    override val rawType: ArgumentType<Vec> =
        PositionArgumentType(blockAligned = false, twoDimensional = false)
    override fun stringify(value: Vec): String = "${value.x()} ${value.y()} ${value.z()}"
}

/**
 * A horizontal position of two coordinates, `x` and `z`, each absolute or `~`-relative.
 *
 * The returned [Vec] is resolved against the sender and has `y` set to `0`. Local coordinates are
 * not accepted.
 */
class Position2DArgument(nodeName: String) : Argument<Vec>(nodeName) {
    override val kind = ArgumentKind.Position2D
    override val rawType: ArgumentType<Vec> =
        PositionArgumentType(blockAligned = false, twoDimensional = true)
    override fun stringify(value: Vec): String = "${value.x()} ${value.z()}"
}

/**
 * A block position of three coordinates, each a whole number or `~`-relative, or three `^` local
 * coordinates.
 *
 * Like [PositionArgument], the returned [Vec] is resolved against the sender. Relative offsets may
 * be decimal, so the result is not guaranteed to be block-aligned.
 */
class BlockPositionArgument(nodeName: String) : Argument<Vec>(nodeName) {
    override val kind = ArgumentKind.BlockPosition
    override val rawType: ArgumentType<Vec> =
        PositionArgumentType(blockAligned = true, twoDimensional = false)
    override fun stringify(value: Vec): String =
        "${floor(value.x()).toInt()} ${floor(value.y()).toInt()} ${floor(value.z()).toInt()}"
}

/**
 * A rotation of yaw followed by pitch, each absolute or `~`-relative to the sender's view.
 *
 * The returned [Rotation] is resolved against the sender; a source that is not an entity has the
 * origin view.
 */
class RotationArgument(nodeName: String) : Argument<Rotation>(nodeName) {
    override val kind = ArgumentKind.Rotation
    override val rawType: ArgumentType<Rotation> = RotationArgumentType
    override fun stringify(value: Rotation): String = "${value.yaw} ${value.pitch}"
}

/**
 * An angle in degrees, absolute or relative to the sender's yaw with `~`, wrapped into
 * `[-180, 180)`.
 */
class AngleArgument(nodeName: String) : Argument<Float>(nodeName) {
    override val kind = ArgumentKind.Angle
    override val rawType: ArgumentType<Float> = AngleParser
    override fun stringify(value: Float): String = value.toString()
}

/**
 * A set of axes written as a swizzle such as `xz`, case-insensitively.
 *
 * An unknown or repeated letter is rejected. The returned set keeps the written order.
 */
class AxisArgument(nodeName: String) : Argument<Set<Axis>>(nodeName) {
    override val kind = ArgumentKind.Axis
    override val rawType: ArgumentType<Set<Axis>> = AxisParser
    override fun stringify(value: Set<Axis>): String =
        value.joinToString("") { axis -> axis.name.lowercase() }
}
