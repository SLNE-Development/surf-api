package dev.slne.surf.api.paper.display.shape

import dev.slne.surf.api.paper.display.render.Canvas
import java.util.BitSet

/**
 * A shape that can rasterize itself into a [BitSet] and paint its own pixels onto a [Canvas].
 */
interface Shape {
    val width: Int
    val height: Int

    fun rasterize(): BitSet

    fun paint(canvas: Canvas, x: Int, y: Int, color: Int) {
        val bits = rasterize()
        val w = width
        var i = bits.nextSetBit(0)
        while (i >= 0) {
            canvas.setPixel(x + i % w, y + i / w, color)
            i = bits.nextSetBit(i + 1)
        }
    }

    companion object {
        fun rectangle(w: Int, h: Int, filled: Boolean = true): Shape = RectangleShape(w, h, filled)
        fun circle(radius: Int, filled: Boolean = true): Shape = CircleShape(radius, filled)
        fun ellipse(radiusX: Int, radiusY: Int, filled: Boolean = true): Shape = EllipseShape(radiusX, radiusY, filled)
        fun line(dx: Int, dy: Int, thickness: Int = 1): Shape = LineShape(dx, dy, thickness)
        fun triangle(w: Int, h: Int, filled: Boolean = true): Shape = TriangleShape(w, h, filled)
        fun roundedRectangle(w: Int, h: Int, cornerRadius: Int, filled: Boolean = true): Shape =
            RoundedRectangleShape(w, h, cornerRadius, filled)
        fun polygon(vararg vertices: Pair<Int, Int>, filled: Boolean = true): Shape =
            PolygonShape(vertices.toList(), filled)
    }
}
