package dev.slne.surf.api.core.algorithms

import org.spongepowered.math.vector.Vector2d
import kotlin.test.Test
import kotlin.test.assertEquals

class ConvexHull2DTest {
    @Test
    fun `single point is its own convex hull`() {
        val point = Vector2d(2.0, 4.0)

        assertEquals(listOf(point), ConvexHull2D.compute(arrayOf(point)))
    }

    @Test
    fun `collection extension keeps all input points available to hull`() {
        val points = linkedSetOf(
            Vector2d(0.0, 0.0),
            Vector2d(2.0, 0.0),
            Vector2d(2.0, 2.0),
            Vector2d(0.0, 2.0),
        )

        assertEquals(4, points.convexHull2D().size)
    }

    @Test
    fun `duplicate coordinates collapse to one hull point`() {
        val point = Vector2d(2.0, 4.0)

        assertEquals(listOf(point), ConvexHull2D.compute(arrayOf(point, point, point)))
    }
}
