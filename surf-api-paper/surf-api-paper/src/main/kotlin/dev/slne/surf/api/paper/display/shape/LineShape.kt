package dev.slne.surf.api.paper.display.shape

import java.util.BitSet
import kotlin.math.abs

class LineShape(
    val dx: Int,
    val dy: Int,
    val thickness: Int = 1
) : Shape {
    override val width = abs(dx) + thickness
    override val height = abs(dy) + thickness

    private val bits: BitSet = BitSet(width * height).apply {
        val x0 = if (dx >= 0) 0 else abs(dx)
        val y0 = if (dy >= 0) 0 else abs(dy)
        val x1 = x0 + dx
        val y1 = y0 + dy

        bresenham(this, x0, y0, x1, y1, thickness)
    }

    private fun bresenham(bits: BitSet, x0: Int, y0: Int, x1: Int, y1: Int, thickness: Int) {
        val dx = abs(x1 - x0)
        val dy = abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx - dy
        var x = x0
        var y = y0
        val half = thickness / 2

        while (true) {
            for (ty in -half until -half + thickness) {
                for (tx in -half until -half + thickness) {
                    val px = x + tx
                    val py = y + ty
                    if (px in 0 until width && py in 0 until height) {
                        bits.set(py * width + px)
                    }
                }
            }

            if (x == x1 && y == y1) break
            val e2 = 2 * err
            if (e2 > -dy) { err -= dy; x += sx }
            if (e2 < dx) { err += dx; y += sy }
        }
    }

    override fun rasterize(): BitSet = bits
}
