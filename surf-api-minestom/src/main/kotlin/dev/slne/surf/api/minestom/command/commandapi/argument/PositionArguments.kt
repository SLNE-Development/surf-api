/*
 * Substantially translated from CommandAPI 12.0.0 (https://github.com/CommandAPI/CommandAPI).
 * MIT License, Copyright (c) 2020 - 2022 Jorel Ali.
 * The complete license is distributed in META-INF/LICENSES/CommandAPI-LICENSE.txt.
 */
package dev.slne.surf.api.minestom.command.commandapi.argument

import com.mojang.brigadier.arguments.ArgumentType
import dev.slne.surf.api.minestom.command.commandapi.argument.parser.AngleParser
import dev.slne.surf.api.minestom.command.commandapi.argument.parser.AxisParser
import dev.slne.surf.api.minestom.command.commandapi.argument.parser.PositionArgumentType
import dev.slne.surf.api.minestom.command.commandapi.argument.parser.RotationArgumentType
import net.minestom.server.coordinate.Vec
import kotlin.math.floor

/**
 * A world axis, as used by swizzle arguments such as `/data get ... xyz`.
 */
enum class Axis {
    X,
    Y,
    Z,
}

/**
 * A yaw/pitch view, as produced by [RotationArgument].
 */
data class Rotation(val yaw: Float, val pitch: Float)

class PositionArgument(nodeName: String) : Argument<Vec>(nodeName) {
    override val kind = ArgumentKind.Position
    override val rawType: ArgumentType<Vec> =
        PositionArgumentType(blockAligned = false, twoDimensional = false)
    override fun stringify(value: Vec): String = "${value.x()} ${value.y()} ${value.z()}"
}

class Position2DArgument(nodeName: String) : Argument<Vec>(nodeName) {
    override val kind = ArgumentKind.Position2D
    override val rawType: ArgumentType<Vec> =
        PositionArgumentType(blockAligned = false, twoDimensional = true)
    override fun stringify(value: Vec): String = "${value.x()} ${value.z()}"
}

class BlockPositionArgument(nodeName: String) : Argument<Vec>(nodeName) {
    override val kind = ArgumentKind.BlockPosition
    override val rawType: ArgumentType<Vec> =
        PositionArgumentType(blockAligned = true, twoDimensional = false)
    override fun stringify(value: Vec): String =
        "${floor(value.x()).toInt()} ${floor(value.y()).toInt()} ${floor(value.z()).toInt()}"
}

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

class AxisArgument(nodeName: String) : Argument<Set<Axis>>(nodeName) {
    override val kind = ArgumentKind.Axis
    override val rawType: ArgumentType<Set<Axis>> = AxisParser
    override fun stringify(value: Set<Axis>): String =
        value.joinToString("") { axis -> axis.name.lowercase() }
}
