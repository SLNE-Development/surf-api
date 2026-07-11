package dev.slne.surf.api.core.math

import org.spongepowered.math.vector.Vector3d
import kotlin.math.abs
import kotlin.math.floor

/**
 * Utility for tracing lines through a voxel grid using a 3D Bresenham-like algorithm.
 *
 * This tracer determines all voxel coordinates that a line intersects between two 3D points,
 * returning them in traversal order from the start point to the end point.
 */
object VoxelLineTracer {

    /**
     * Traces directly into [destination] without allocating a coroutine-backed [Sequence].
     * This is preferable when the complete result is needed immediately.
     */
    fun <C : MutableCollection<in Vector3d>> traceTo(
        p0: Vector3d,
        p1: Vector3d,
        destination: C,
    ): C {
        var x = floor(p0.x()).toInt()
        var y = floor(p0.y()).toInt()
        var z = floor(p0.z()).toInt()
        val targetX = floor(p1.x()).toInt()
        val targetY = floor(p1.y()).toInt()
        val targetZ = floor(p1.z()).toInt()
        val dx = abs(targetX - x)
        val sx = targetX.compareTo(x)
        val dy = abs(targetY - y)
        val sy = targetY.compareTo(y)
        val dz = abs(targetZ - z)
        val sz = targetZ.compareTo(z)

        if (dx >= dy && dx >= dz) {
            var errY = 2 * dy - dx
            var errZ = 2 * dz - dx
            while (x != targetX) {
                destination.add(Vector3d(x.toDouble(), y.toDouble(), z.toDouble()))
                if (errY >= 0) {
                    y += sy
                    errY -= 2 * dx
                }
                if (errZ >= 0) {
                    z += sz
                    errZ -= 2 * dx
                }
                errY += 2 * dy
                errZ += 2 * dz
                x += sx
            }
        } else if (dy >= dx && dy >= dz) {
            var errX = 2 * dx - dy
            var errZ = 2 * dz - dy
            while (y != targetY) {
                destination.add(Vector3d(x.toDouble(), y.toDouble(), z.toDouble()))
                if (errX >= 0) {
                    x += sx
                    errX -= 2 * dy
                }
                if (errZ >= 0) {
                    z += sz
                    errZ -= 2 * dy
                }
                errX += 2 * dx
                errZ += 2 * dz
                y += sy
            }
        } else {
            var errX = 2 * dx - dz
            var errY = 2 * dy - dz
            while (z != targetZ) {
                destination.add(Vector3d(x.toDouble(), y.toDouble(), z.toDouble()))
                if (errX >= 0) {
                    x += sx
                    errX -= 2 * dz
                }
                if (errY >= 0) {
                    y += sy
                    errY -= 2 * dz
                }
                errX += 2 * dx
                errY += 2 * dy
                z += sz
            }
        }
        destination.add(Vector3d(targetX.toDouble(), targetY.toDouble(), targetZ.toDouble()))
        return destination
    }

    /**
     * Traces a line between two 3D points and returns all voxel coordinates along the path.
     *
     * Uses a 3D integer-based line algorithm to efficiently determine which discrete voxel
     * positions a continuous line passes through. The sequence includes both endpoints and
     * all intermediate voxels in traversal order.
     *
     * @param p0 The starting point of the line.
     * @param p1 The ending point of the line.
     * @return A lazy sequence of voxel coordinates from [p0] to [p1].
     *
     * Example:
     * ```kotlin
     * val start = Vector3d(0.0, 0.0, 0.0)
     * val end = Vector3d(3.0, 2.0, 1.0)
     * val voxels = VoxelLineTracer.trace(start, end).toList()
     * // Returns: [(0,0,0), (1,0,0), (1,1,0), (2,1,1), (3,2,1)]
     * ```
     */
    fun trace(p0: Vector3d, p1: Vector3d): Sequence<Vector3d> = sequence {
        var x = floor(p0.x()).toInt()
        var y = floor(p0.y()).toInt()
        var z = floor(p0.z()).toInt()
        val targetX = floor(p1.x()).toInt()
        val targetY = floor(p1.y()).toInt()
        val targetZ = floor(p1.z()).toInt()
        val dx = abs(targetX - x)
        val sx = targetX.compareTo(x)
        val dy = abs(targetY - y)
        val sy = targetY.compareTo(y)
        val dz = abs(targetZ - z)
        val sz = targetZ.compareTo(z)

        if (dx >= dy && dx >= dz) {
            var errY = 2 * dy - dx
            var errZ = 2 * dz - dx
            while (x != targetX) {
                yield(Vector3d(x.toDouble(), y.toDouble(), z.toDouble()))
                if (errY >= 0) {
                    y += sy; errY -= 2 * dx
                }
                if (errZ >= 0) {
                    z += sz; errZ -= 2 * dx
                }
                errY += 2 * dy; errZ += 2 * dz; x += sx
            }
        } else if (dy >= dx && dy >= dz) {
            var errX = 2 * dx - dy
            var errZ = 2 * dz - dy
            while (y != targetY) {
                yield(Vector3d(x.toDouble(), y.toDouble(), z.toDouble()))
                if (errX >= 0) {
                    x += sx; errX -= 2 * dy
                }
                if (errZ >= 0) {
                    z += sz; errZ -= 2 * dy
                }
                errX += 2 * dx; errZ += 2 * dz; y += sy
            }
        } else {
            var errX = 2 * dx - dz
            var errY = 2 * dy - dz
            while (z != targetZ) {
                yield(Vector3d(x.toDouble(), y.toDouble(), z.toDouble()))
                if (errX >= 0) {
                    x += sx; errX -= 2 * dz
                }
                if (errY >= 0) {
                    y += sy; errY -= 2 * dz
                }
                errX += 2 * dx; errY += 2 * dy; z += sz
            }
        }
        yield(Vector3d(targetX.toDouble(), targetY.toDouble(), targetZ.toDouble()))
    }
}
