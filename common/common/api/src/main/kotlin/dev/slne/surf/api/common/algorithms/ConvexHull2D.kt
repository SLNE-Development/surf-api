package dev.slne.surf.api.common.algorithms

import org.spongepowered.math.vector.Vector2d
import org.spongepowered.math.vector.Vector3d
import org.spongepowered.math.vector.Vector4d
import org.spongepowered.math.vector.Vectord

/**
 * @see <a href="https://rosettacode.org/wiki/Convex_hull#Kotlin">Rosetta Code</a>
 * @see <a href="https://en.wikipedia.org/wiki/Convex_hull">Wikipedia</a>
 */
object ConvexHull2D {
    fun <V : Vectord> compute(points: Array<V>): List<V> {
        if (points.isEmpty()) return emptyList()
        val points = points.sortedWith(Comparator<V> { a, b ->
            val x = a.x().compareTo(b.x())
            if (x == 0) {
                a.z().compareTo(b.z())
            } else {
                x
            }
        }).toMutableList()
        val hull = mutableListOf<V>()

        // lower hull
        for (point in points) {
            while (hull.size >= 2 && !ccw(hull[hull.size - 2], hull.last(), point)) {
                hull.removeAt(hull.lastIndex)
            }
            hull.add(point)
        }

        // upper hull
        val lowerSize = hull.size + 1
        for (i in points.size - 2 downTo 0) {
            val point = points[i]
            while (hull.size >= lowerSize && !ccw(hull[hull.size - 2], hull.last(), point)) {
                hull.removeAt(hull.lastIndex)
            }
            hull.add(point)
        }

        // Remove the last point because it is the same as the first one
        hull.removeAt(hull.lastIndex)
        return hull
    }

    private fun <V : Vectord> V.x(): Double = when (this) {
        is Vector2d -> x()
        is Vector3d -> x()
        is Vector4d -> x()
        else -> error("Unsupported vector type: ${this::class.simpleName}")
    }

    private fun <V : Vectord> V.z(): Double = when (this) {
        is Vector2d -> y()
        is Vector3d -> z()
        is Vector4d -> z()
        else -> error("Unsupported vector type: ${this::class.simpleName}")
    }

    /* ccw returns true if the three points make a counter-clockwise turn */
    fun ccw(a: Vectord, b: Vectord, c: Vectord) =
        ((b.x() - a.x()) * (c.z() - a.z())) > ((b.z() - a.z()) * (c.x() - a.x()))
}

fun <V : Vectord> Array<V>.convexHull2D() = ConvexHull2D.compute(this)
inline fun <reified V : Vectord> Iterable<V>.convexHull2D() = toList().toTypedArray().convexHull2D()