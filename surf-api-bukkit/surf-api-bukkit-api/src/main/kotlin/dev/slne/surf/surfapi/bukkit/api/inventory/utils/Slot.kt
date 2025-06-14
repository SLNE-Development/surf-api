package dev.slne.surf.surfapi.bukkit.api.inventory.utils

import org.jetbrains.annotations.Range

interface Slot {
    fun getX(length: Int): Int
    fun getY(length: Int): Int

    companion object {
        fun fromXY(x: Int, y: Int): Slot = XY(x, y)
        fun fromIndex(index: Int): Slot = Indexed(index)

        operator fun invoke(
            x: @Range(from = 0, to = 8) Int,
            y: @Range(from = 0, to = 5) Int,
        ): Slot {
            return fromXY(x, y)
        }

        operator fun invoke(index: Int): Slot {
            return fromIndex(index)
        }
    }
}

internal data class XY(val x: Int, val y: Int) : Slot {
    override fun getX(length: Int) = x
    override fun getY(length: Int) = y
}

internal data class Indexed(val index: Int) : Slot {
    override fun getX(length: Int): Int {
        require(length > 0) { "Length must be greater than 0" }
        return index % length
    }

    override fun getY(length: Int): Int {
        require(length > 0) { "Length must be greater than 0" }
        return index / length
    }
}

fun slot(x: @Range(from = 0, to = 8) Int, y: @Range(from = 0, to = 5) Int) = Slot.fromXY(x, y)
fun slot(index: Int) = Slot.fromIndex(index)