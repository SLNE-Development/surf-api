package dev.slne.surf.api.paper.display.shape

import java.util.BitSet

class RectangleShape(
    override val width: Int,
    override val height: Int,
    val filled: Boolean = true
) : Shape {
    private val bits: BitSet = BitSet(width * height).apply {
        if (filled) {
            set(0, width * height)
        } else {
            for (x in 0 until width) {
                set(x)
                set((height - 1) * width + x)
            }
            for (y in 0 until height) {
                set(y * width)
                set(y * width + width - 1)
            }
        }
    }

    override fun rasterize(): BitSet = bits
}
