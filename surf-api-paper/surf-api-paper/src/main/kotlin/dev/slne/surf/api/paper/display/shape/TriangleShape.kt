package dev.slne.surf.api.paper.display.shape

import java.util.BitSet

class TriangleShape(
    override val width: Int,
    override val height: Int,
    val filled: Boolean = true
) : Shape {

    private val bits: BitSet = BitSet(width * height).apply {
        val apexX = width / 2
        val apexY = 0
        val leftX = 0
        val leftY = height - 1
        val rightX = width - 1
        val rightY = height - 1

        if (filled) {
            for (y in 0 until height) {
                val progress = if (height > 1) y.toFloat() / (height - 1) else 1f
                val xLeft = (apexX + (leftX - apexX) * progress).toInt()
                val xRight = (apexX + (rightX - apexX) * progress).toInt()
                for (x in xLeft..xRight) {
                    if (x in 0 until width) {
                        set(y * width + x)
                    }
                }
            }
        } else {
            drawLine(this, apexX, apexY, leftX, leftY)
            drawLine(this, apexX, apexY, rightX, rightY)
            drawLine(this, leftX, leftY, rightX, rightY)
        }
    }

    private fun drawLine(bits: BitSet, x0: Int, y0: Int, x1: Int, y1: Int) {
        val dx = kotlin.math.abs(x1 - x0)
        val dy = kotlin.math.abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx - dy
        var x = x0
        var y = y0

        while (true) {
            if (x in 0 until width && y in 0 until height) {
                bits.set(y * width + x)
            }
            if (x == x1 && y == y1) break
            val e2 = 2 * err
            if (e2 > -dy) { err -= dy; x += sx }
            if (e2 < dx) { err += dx; y += sy }
        }
    }

    override fun rasterize(): BitSet = bits
}
