package dev.slne.surf.api.paper.display.shape

import java.util.BitSet
import kotlin.math.abs

class PolygonShape(
    val vertices: List<Pair<Int, Int>>,
    val filled: Boolean = true
) : Shape {
    override val width: Int
    override val height: Int

    private val bits: BitSet

    init {
        require(vertices.size >= 3) { "A polygon requires at least 3 vertices" }

        val minX = vertices.minOf { it.first }
        val minY = vertices.minOf { it.second }
        val maxX = vertices.maxOf { it.first }
        val maxY = vertices.maxOf { it.second }

        width = maxX - minX + 1
        height = maxY - minY + 1

        val normalized = vertices.map { (it.first - minX) to (it.second - minY) }

        bits = BitSet(width * height).apply {
            if (filled) {
                for (y in 0 until height) {
                    val intersections = mutableListOf<Int>()
                    val n = normalized.size
                    for (i in 0 until n) {
                        val (x0, y0) = normalized[i]
                        val (x1, y1) = normalized[(i + 1) % n]
                        if ((y0 <= y && y1 > y) || (y1 <= y && y0 > y)) {
                            val xIntersect = x0 + (y - y0).toFloat() / (y1 - y0) * (x1 - x0)
                            intersections.add(xIntersect.toInt())
                        }
                    }
                    intersections.sort()
                    for (i in 0 until intersections.size - 1 step 2) {
                        for (x in intersections[i]..intersections[i + 1]) {
                            if (x in 0 until width) {
                                set(y * width + x)
                            }
                        }
                    }
                }
            } else {
                val n = normalized.size
                for (i in 0 until n) {
                    val (x0, y0) = normalized[i]
                    val (x1, y1) = normalized[(i + 1) % n]
                    drawLine(this, x0, y0, x1, y1)
                }
            }
        }
    }

    private fun drawLine(bits: BitSet, x0: Int, y0: Int, x1: Int, y1: Int) {
        val dx = abs(x1 - x0)
        val dy = abs(y1 - y0)
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
