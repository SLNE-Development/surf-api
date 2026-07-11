package dev.slne.surf.api.core.algorithms

import dev.slne.surf.api.core.util.emptyObjectList
import dev.slne.surf.api.core.util.mutableObjectListOf
import it.unimi.dsi.fastutil.objects.ObjectList
import org.spongepowered.math.vector.Vector2d
import org.spongepowered.math.vector.Vector3d
import org.spongepowered.math.vector.Vector4d
import org.spongepowered.math.vector.Vectord

/**
 * @see <a href="https://rosettacode.org/wiki/Convex_hull#Kotlin">Rosetta Code</a>
 * @see <a href="https://en.wikipedia.org/wiki/Convex_hull">Wikipedia</a>
 */
object ConvexHull2D {
    //  val sorted = list.sortedWith(compareBy<Vec2> { it.x }.thenBy { it.z }).toMutableObjectList()
    fun <V : Vectord> compute(points: Array<V>): ObjectList<V> {
        if (points.isEmpty()) return emptyObjectList()
        if (points.size == 1) return mutableObjectListOf(points[0])

        val sortedPoints = points.copyOf()
        sortedPoints.sortWith(Comparator<V> { a, b ->
            val x = a.x().compareTo(b.x())
            if (x == 0) {
                a.z().compareTo(b.z())
            } else {
                x
            }
        })

        var uniqueSize = 0
        for (point in sortedPoints) {
            if (uniqueSize == 0) {
                sortedPoints[uniqueSize++] = point
                continue
            }
            val previous = sortedPoints[uniqueSize - 1]
            if (previous.x() != point.x() || previous.z() != point.z()) {
                sortedPoints[uniqueSize++] = point
            }
        }
        if (uniqueSize == 1) return mutableObjectListOf(sortedPoints[0])

        val hull = mutableObjectListOf<V>(uniqueSize)

        // lower hull
        for (i in 0 until uniqueSize) {
            val point = sortedPoints[i]
            while (hull.size >= 2 && !ccw(hull[hull.size - 2], hull.last(), point)) {
                hull.removeLast()
            }
            hull.add(point)
        }

        // upper hull
        val lowerSize = hull.size + 1
        for (i in uniqueSize - 2 downTo 0) {
            val point = sortedPoints[i]
            while (hull.size >= lowerSize && !ccw(hull[hull.size - 2], hull.last(), point)) {
                hull.removeLast()
            }
            hull.add(point)
        }

        // Remove the last point because it is the same as the first one
        hull.removeLast()
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
inline fun <reified V : Vectord> Iterable<V>.convexHull2D() =
    if (this is Collection<V>) {
        toTypedArray().convexHull2D()
    } else {
        toList().toTypedArray().convexHull2D()
    }
