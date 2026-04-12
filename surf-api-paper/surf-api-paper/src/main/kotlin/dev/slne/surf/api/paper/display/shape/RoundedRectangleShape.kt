package dev.slne.surf.api.paper.display.shape

import java.util.BitSet
import kotlin.math.min

class RoundedRectangleShape(
    override val width: Int,
    override val height: Int,
    val cornerRadius: Int,
    val filled: Boolean = true
) : Shape {
    private val bits: BitSet = BitSet(width * height).apply {
        val r = min(cornerRadius, min(width / 2, height / 2))

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (isInsideRoundedRect(x, y, r)) {
                    if (filled) {
                        set(y * width + x)
                    } else {
                        if (!isInsideRoundedRect(x, y, r, inset = 1)) {
                            set(y * width + x)
                        }
                    }
                }
            }
        }
    }

    private fun isInsideRoundedRect(x: Int, y: Int, r: Int, inset: Int = 0): Boolean {
        val w = width - inset * 2
        val h = height - inset * 2
        val px = x - inset
        val py = y - inset

        if (px < 0 || py < 0 || px >= w || py >= h) return false
        if (r <= 0) return true

        val effectiveR = min(r, min(w / 2, h / 2))

        val cornerX: Int
        val cornerY: Int

        when {
            px < effectiveR && py < effectiveR -> { cornerX = effectiveR; cornerY = effectiveR }
            px >= w - effectiveR && py < effectiveR -> { cornerX = w - effectiveR - 1; cornerY = effectiveR }
            px < effectiveR && py >= h - effectiveR -> { cornerX = effectiveR; cornerY = h - effectiveR - 1 }
            px >= w - effectiveR && py >= h - effectiveR -> { cornerX = w - effectiveR - 1; cornerY = h - effectiveR - 1 }
            else -> return true
        }

        val dx = px - cornerX
        val dy = py - cornerY
        return dx * dx + dy * dy <= effectiveR * effectiveR
    }

    override fun rasterize(): BitSet = bits
}
