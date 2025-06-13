package dev.slne.surf.surfapi.bukkit.api.inventory.utils

import it.unimi.dsi.fastutil.ints.AbstractInt2IntMap
import it.unimi.dsi.fastutil.ints.Int2IntMap

internal object GeometryUtils {

    fun processClockwiseRotation(
        x: Int,
        y: Int,
        length: Int,
        height: Int,
        rotation: Int,
    ): Int2IntMap.Entry {
        var newX = x
        var newY = y

        when (rotation) {
            90 -> {
                newX = height - 1 - y
                newY = x
            }

            180 -> {
                newX = length - 1 - x
                newY = height - 1 - y
            }

            270 -> {
                newX = y
                newY = length - 1 - x
            }
        }

        return AbstractInt2IntMap.BasicEntry(newX, newY)
    }

    fun processCounterClockwiseRotation(
        x: Int, y: Int, length: Int, height: Int,
        rotation: Int,
    ): MutableMap.MutableEntry<Int?, Int?> {
        return processClockwiseRotation(x, y, length, height, 360 - rotation)
    }
}