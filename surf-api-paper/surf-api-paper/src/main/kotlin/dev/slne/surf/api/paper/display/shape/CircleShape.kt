package dev.slne.surf.api.paper.display.shape

import java.util.BitSet

class CircleShape(
    val radius: Int,
    val filled: Boolean = true
) : Shape {
    override val width = radius * 2 + 1
    override val height = radius * 2 + 1

    private val bits: BitSet = BitSet(width * height).apply {
        val cx = radius
        val cy = radius
        if (filled) {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val dx = x - cx
                    val dy = y - cy
                    if (dx * dx + dy * dy <= radius * radius) {
                        set(y * width + x)
                    }
                }
            }
        } else {
            var x = radius
            var y = 0
            var d = 1 - radius
            while (x >= y) {
                setSymmetric(this, cx, cy, x, y)
                y++
                if (d <= 0) {
                    d += 2 * y + 1
                } else {
                    x--
                    d += 2 * y - 2 * x + 1
                }
            }
        }
    }

    private fun setSymmetric(bits: BitSet, cx: Int, cy: Int, x: Int, y: Int) {
        val w = width
        fun set(px: Int, py: Int) {
            if (px in 0 until w && py in 0 until height) {
                bits.set(py * w + px)
            }
        }
        set(cx + x, cy + y)
        set(cx - x, cy + y)
        set(cx + x, cy - y)
        set(cx - x, cy - y)
        set(cx + y, cy + x)
        set(cx - y, cy + x)
        set(cx + y, cy - x)
        set(cx - y, cy - x)
    }

    override fun rasterize(): BitSet = bits
}
