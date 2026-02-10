package dev.slne.surf.surfapi.core.api.math

import org.spongepowered.math.vector.Vector3d
import kotlin.math.abs
import kotlin.math.sign

/**
 * Utility for tracing lines through a voxel grid using a 3D Bresenham-like algorithm.
 *
 * This tracer determines all voxel coordinates that a line intersects between two 3D points,
 * returning them in traversal order from the start point to the end point.
 */
object VoxelLineTracer {

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
        var x = p0.x();
        var y = p0.y();
        var z = p0.z()
        val dx = abs(p1.x() - x);
        val sx = sign(p1.x() - x)
        val dy = abs(p1.y() - y);
        val sy = sign(p1.y() - y)
        val dz = abs(p1.z() - z);
        val sz = sign(p1.z() - z)

        if (dx >= dy && dx >= dz) {
            var errY = 2 * dy - dx
            var errZ = 2 * dz - dx
            while (x != p1.x()) {
                yield(Vector3d(x, y, z))
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
            while (y != p1.y()) {
                yield(Vector3d(x, y, z))
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
            while (z != p1.z()) {
                yield(Vector3d(x, y, z))
                if (errX >= 0) {
                    x += sx; errX -= 2 * dz
                }
                if (errY >= 0) {
                    y += sy; errY -= 2 * dz
                }
                errX += 2 * dx; errY += 2 * dy; z += sz
            }
        }
        yield(p1)
    }
}