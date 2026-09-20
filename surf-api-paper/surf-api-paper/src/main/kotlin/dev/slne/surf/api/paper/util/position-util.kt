@file:Suppress("UnstableApiUsage")

package dev.slne.surf.api.paper.util

import io.papermc.paper.math.BlockPosition
import io.papermc.paper.math.Position
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Calculates the squared Euclidean distance between this position and [other].
 *
 * This avoids the square-root operation required for the actual distance and is
 * therefore preferable when only comparing distances.
 *
 * @param other the position to measure the distance to
 * @return the squared Euclidean distance to [other]
 */
fun Position.distanceSquared(other: Position): Double {
    val dx = x() - other.x()
    val dy = y() - other.y()
    val dz = z() - other.z()

    return dx * dx + dy * dy + dz * dz
}

/**
 * Calculates the squared Euclidean distance between this block position and [other].
 *
 * @param other the block position to measure the distance to
 * @return the squared Euclidean distance to [other]
 */
fun BlockPosition.distanceSquared(other: BlockPosition): Long {
    val dx = blockX().toLong() - other.blockX().toLong()
    val dy = blockY().toLong() - other.blockY().toLong()
    val dz = blockZ().toLong() - other.blockZ().toLong()

    return dx * dx + dy * dy + dz * dz
}

/**
 * Calculates the Euclidean distance between this position and [other].
 *
 * @param other the position to measure the distance to
 * @return the Euclidean distance to [other]
 */
fun Position.distance(other: Position): Double {
    return sqrt(distanceSquared(other))
}

fun Position.horizontalDistanceSquared(other: Position): Double {
    val dx = x() - other.x()
    val dz = z() - other.z()

    return dx * dx + dz * dz
}

/**
 * Calculates the squared horizontal distance between this block position and [other].
 *
 * Only the X and Z coordinates are considered.
 *
 * @param other the block position to measure the distance to
 * @return the squared horizontal distance to [other]
 */
fun BlockPosition.horizontalDistanceSquared(other: BlockPosition): Long {
    val dx = blockX().toLong() - other.blockX().toLong()
    val dz = blockZ().toLong() - other.blockZ().toLong()

    return dx * dx + dz * dz
}

/**
 * Calculates the horizontal distance between this position and [other].
 *
 * Only the X and Z coordinates are considered.
 *
 * @param other the position to measure the distance to
 * @return the horizontal distance to [other]
 */
fun Position.horizontalDistance(other: Position): Double {
    return sqrt(horizontalDistanceSquared(other))
}

/**
 * Calculates the absolute vertical distance between this position and [other].
 *
 * Only the Y coordinate is considered.
 *
 * @param other the position to measure the distance to
 * @return the absolute difference between the Y coordinates
 */
fun Position.verticalDistance(other: Position): Double {
    return abs(y() - other.y())
}

/**
 * Calculates the absolute vertical distance between this block position and [other].
 *
 * Only the Y coordinate is considered.
 *
 * @param other the block position to measure the distance to
 * @return the absolute difference between the block Y coordinates
 */
fun BlockPosition.verticalDistance(other: BlockPosition): Long {
    return abs(blockY().toLong() - other.blockY().toLong())
}

/**
 * Checks whether this position is within the specified Euclidean [distance] of [other].
 *
 * The boundary is inclusive. A negative [distance] always produces `false`.
 *
 * @param other the position to compare against
 * @param distance the maximum allowed distance
 * @return `true` if this position is within [distance] of [other], otherwise `false`
 */
fun Position.isWithinDistance(other: Position, distance: Double): Boolean {
    return distance >= 0.0 && distanceSquared(other) <= distance * distance
}

/**
 * Calculates the Manhattan distance between this block position and [other].
 *
 * The Manhattan distance is the sum of the absolute differences along the X, Y,
 * and Z axes.
 *
 * @param other the block position to measure the distance to
 * @return the Manhattan distance to [other]
 */
fun BlockPosition.manhattanDistance(other: BlockPosition): Long {
    val dx = abs(blockX().toLong() - other.blockX().toLong())
    val dy = abs(blockY().toLong() - other.blockY().toLong())
    val dz = abs(blockZ().toLong() - other.blockZ().toLong())

    return dx + dy + dz
}

/**
 * Calculates the Chebyshev distance between this block position and [other].
 *
 * The Chebyshev distance is the greatest absolute difference along any of the
 * X, Y, or Z axes.
 *
 * @param other the block position to measure the distance to
 * @return the Chebyshev distance to [other]
 */
fun BlockPosition.chebyshevDistance(other: BlockPosition): Long {
    val dx = abs(blockX().toLong() - other.blockX().toLong())
    val dy = abs(blockY().toLong() - other.blockY().toLong())
    val dz = abs(blockZ().toLong() - other.blockZ().toLong())

    return max(max(dx, dy), dz)
}

/**
 * Checks whether this block position and [other] are located in the same chunk.
 *
 * Only the chunk X and Z coordinates are compared.
 *
 * @param other the block position to compare against
 * @return `true` if both positions belong to the same chunk, otherwise `false`
 */
fun BlockPosition.isSameChunk(other: BlockPosition): Boolean {
    return chunkX == other.chunkX && chunkZ == other.chunkZ
}

/**
 * Checks whether this position and [other] refer to the same block.
 *
 * The positions may contain different fractional coordinates as long as their
 * block X, Y, and Z coordinates are equal.
 *
 * @param other the position to compare against
 * @return `true` if both positions refer to the same block, otherwise `false`
 */
fun Position.isSameBlock(other: Position): Boolean {
    return blockX() == other.blockX() &&
            blockY() == other.blockY() &&
            blockZ() == other.blockZ()
}

/**
 * Calculates the signed X-axis offset from this position to [other].
 *
 * @param other the target position
 * @return `other.x() - x()`
 */
fun Position.deltaX(other: Position): Double = other.x() - x()

/**
 * Calculates the signed Y-axis offset from this position to [other].
 *
 * @param other the target position
 * @return `other.y() - y()`
 */
fun Position.deltaY(other: Position): Double = other.y() - y()

/**
 * Calculates the signed Z-axis offset from this position to [other].
 *
 * @param other the target position
 * @return `other.z() - z()`
 */
fun Position.deltaZ(other: Position): Double = other.z() - z()

/**
 * Calculates the signed block X-axis offset from this position to [other].
 *
 * @param other the target block position
 * @return `other.blockX() - blockX()`
 */
fun BlockPosition.deltaBlockX(other: BlockPosition): Long =
    other.blockX().toLong() - blockX().toLong()

/**
 * Calculates the signed block Y-axis offset from this position to [other].
 *
 * @param other the target block position
 * @return `other.blockY() - blockY()`
 */
fun BlockPosition.deltaBlockY(other: BlockPosition): Long =
    other.blockY().toLong() - blockY().toLong()

/**
 * Calculates the signed block Z-axis offset from this position to [other].
 *
 * @param other the target block position
 * @return `other.blockZ() - blockZ()`
 */
fun BlockPosition.deltaBlockZ(other: BlockPosition): Long =
    other.blockZ().toLong() - blockZ().toLong()

/**
 * Checks whether this position lies inside the axis-aligned box defined by
 * [first] and [second].
 *
 * The order of the corner positions does not matter, and all boundaries are
 * inclusive.
 *
 * @param first one corner of the box
 * @param second the opposite corner of the box
 * @return `true` if this position lies inside or on the boundary of the box
 */
fun Position.isInside(
    first: Position,
    second: Position,
): Boolean {
    return x() >= min(first.x(), second.x()) &&
            x() <= max(first.x(), second.x()) &&
            y() >= min(first.y(), second.y()) &&
            y() <= max(first.y(), second.y()) &&
            z() >= min(first.z(), second.z()) &&
            z() <= max(first.z(), second.z())
}

/**
 * Checks whether this block position lies within an axis-aligned cube around
 * [center].
 *
 * The [radius] is applied independently to the X, Y, and Z axes, and the
 * boundary is inclusive. A negative [radius] always produces `false`.
 *
 * @param center the center of the cube
 * @param radius the maximum block offset from [center] on each axis
 * @return `true` if this position lies within the cube, otherwise `false`
 */
fun BlockPosition.isWithinCube(
    center: BlockPosition,
    radius: Int,
): Boolean {
    return radius >= 0 &&
            abs(blockX().toLong() - center.blockX()) <= radius &&
            abs(blockY().toLong() - center.blockY()) <= radius &&
            abs(blockZ().toLong() - center.blockZ()) <= radius
}

/**
 * Checks whether this block position lies within an axis-aligned horizontal
 * square around [center].
 *
 * Only the X and Z axes are considered. The [radius] is applied independently
 * to both axes, and the boundary is inclusive. A negative [radius] always
 * produces `false`.
 *
 * @param center the center of the square
 * @param radius the maximum block offset from [center] on the X and Z axes
 * @return `true` if this position lies within the square, otherwise `false`
 */
fun BlockPosition.isWithinSquare(
    center: BlockPosition,
    radius: Int,
): Boolean {
    return radius >= 0 &&
            abs(blockX().toLong() - center.blockX()) <= radius &&
            abs(blockZ().toLong() - center.blockZ()) <= radius
}

/**
 * Returns this block position as a human-readable coordinate string.
 *
 * The coordinates are formatted as `x, y, z` using their integer block values.
 *
 * @return this position formatted as `x, y, z`
 */
fun BlockPosition.readableString(): String {
    return "${blockX()}, ${blockY()}, ${blockZ()}"
}

/**
 * Returns this position as a human-readable coordinate string.
 *
 * Each coordinate is formatted with exactly [decimals] decimal places using
 * [Locale.ROOT], producing a locale-independent representation in the form
 * `x, y, z`.
 *
 * @param decimals the number of decimal places to include for each coordinate
 * @return this position formatted as `x, y, z`
 * @throws IllegalArgumentException if [decimals] is negative
 */
fun Position.readableString(decimals: Int = 2): String {
    require(decimals >= 0) { "Decimals must be non-negative" }

    return String.format(
        Locale.ROOT,
        "%.${decimals}f, %.${decimals}f, %.${decimals}f",
        x(),
        y(),
        z(),
    )
}

private const val BLOCK_POSITION_XZ_MASK = 0x3FFFFFFL
private const val BLOCK_POSITION_Y_MASK = 0xFFFL

/**
 * Packs this block position into a single [Long].
 *
 * The X and Z coordinates use 26 bits each, while the Y coordinate uses 12 bits.
 * The resulting layout is compatible with the standard packed block-position
 * representation:
 *
 * `XXXXXXXX...XXXXXXXX ZZZZZZZZ...ZZZZZZZZ YYYYYYYYYYYY`
 *
 * @return the packed representation of this block position
 */
fun BlockPosition.asLong(): Long {
    return ((blockX().toLong() and BLOCK_POSITION_XZ_MASK) shl 38) or
            ((blockZ().toLong() and BLOCK_POSITION_XZ_MASK) shl 12) or
            (blockY().toLong() and BLOCK_POSITION_Y_MASK)
}

/**
 * Extracts the signed X coordinate from this packed block position.
 *
 * @return the unpacked block X coordinate
 */
fun Long.unpackBlockX(): Int {
    return (this shr 38).toInt()
}

/**
 * Extracts the signed Y coordinate from this packed block position.
 *
 * @return the unpacked block Y coordinate
 */
fun Long.unpackBlockY(): Int {
    return (this shl 52 shr 52).toInt()
}

/**
 * Extracts the signed Z coordinate from this packed block position.
 *
 * @return the unpacked block Z coordinate
 */
fun Long.unpackBlockZ(): Int {
    return (this shl 26 shr 38).toInt()
}

/**
 * Converts this packed block-position value into a [BlockPosition].
 *
 * The X, Y, and Z coordinates are extracted using [unpackBlockX],
 * [unpackBlockY], and [unpackBlockZ], respectively.
 *
 * @return the block position represented by this packed value
 */
fun Long.toBlockPosition(): BlockPosition {
    return Position.block(
        unpackBlockX(),
        unpackBlockY(),
        unpackBlockZ(),
    )
}