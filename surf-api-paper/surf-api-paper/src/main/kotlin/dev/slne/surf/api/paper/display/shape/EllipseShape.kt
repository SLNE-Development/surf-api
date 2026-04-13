package dev.slne.surf.api.paper.display.shape

import java.util.BitSet

class EllipseShape(
    val radiusX: Int,
    val radiusY: Int,
    val filled: Boolean = true
) : Shape {
    override val width = radiusX * 2 + 1
    override val height = radiusY * 2 + 1

    private val bits: BitSet = BitSet(width * height).apply {
        val cx = radiusX
        val cy = radiusY
        val rx2 = radiusX.toLong() * radiusX
        val ry2 = radiusY.toLong() * radiusY

        if (filled) {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val dx = (x - cx).toLong()
                    val dy = (y - cy).toLong()
                    if (dx * dx * ry2 + dy * dy * rx2 <= rx2 * ry2) {
                        set(y * width + x)
                    }
                }
            }
        } else {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val dx = (x - cx).toLong()
                    val dy = (y - cy).toLong()
                    val dist = dx * dx * ry2 + dy * dy * rx2
                    val threshold = rx2 * ry2
                    if (dist <= threshold && dist >= threshold - (rx2 + ry2) * 2) {
                        set(y * width + x)
                    }
                }
            }
        }
    }

    override fun rasterize(): BitSet = bits
}
