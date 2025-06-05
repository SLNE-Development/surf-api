package dev.slne.surf.surfapi.core.api.math

import glm_.vec3.Vec3i
import kotlin.math.abs
import kotlin.math.sign

typealias BlockVec = Vec3i

object VoxelLineTracer {

    fun trace(from: BlockVec, to: BlockVec): Sequence<BlockVec> = sequence {
        var (x, y, z) = from
        val (dx, dy, dz) = to - from
        val stepX = dx.sign
        val stepY = dy.sign
        val stepZ = dz.sign
        val absDX = abs(dx)
        val absDY = abs(dy)
        val absDZ = abs(dz)
        val max = maxOf(absDX, absDY, absDZ).coerceAtLeast(1)

        var tMaxX = halfCell(absDX, from.x, stepX, max)
        var tMaxY = halfCell(absDY, from.y, stepY, max)
        var tMaxZ = halfCell(absDZ, from.z, stepZ, max)

        repeat(max + 1) {
            yield(BlockVec(x, y, z))
            when {
                tMaxX <= tMaxY && tMaxX <= tMaxZ -> {
                    x += stepX
                    tMaxX += absDX
                }

                tMaxY <= tMaxZ -> {
                    y += stepY
                    tMaxY += absDY
                }

                else -> {
                    z += stepZ
                    tMaxZ += absDZ
                }
            }
        }
    }

    @JvmStatic
    private fun halfCell(delta: Int, coord: Int, step: Int, max: Int): Int {
        if (delta == 0) return Int.MAX_VALUE
        val next = if (step > 0) coord + 1 else coord
        return (next - coord) * max
    }
}