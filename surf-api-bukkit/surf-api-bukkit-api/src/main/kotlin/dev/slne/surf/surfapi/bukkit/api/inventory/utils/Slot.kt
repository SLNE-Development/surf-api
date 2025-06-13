package dev.slne.surf.surfapi.bukkit.api.inventory.utils

import org.jetbrains.annotations.Range
import java.util.*

interface Slot {

    fun getX(length: Int): Int
    fun getY(length: Int): Int

    companion object {
        fun fromXY(x: Int, y: Int) = XY(x, y)
        fun fromIndex(index: Int) = Indexed(index)
    }

    class XY(val x: Int, val y: Int) : Slot {
        override fun getX(length: Int) = x
        override fun getY(length: Int) = y

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is XY) return false

            return x == other.x && y == other.y
        }

        override fun hashCode() = Objects.hash(x, y)

        override fun toString() = "XY(x=$x, y=$y)"

    }

    class Indexed(val index: Int) : Slot {
        override fun getX(length: Int): Int {
            if (length <= 0) throw IllegalArgumentException("Length must be greater than 0")

            return index % length
        }

        override fun getY(length: Int): Int {
            if (length <= 0) throw IllegalArgumentException("Length must be greater than 0")

            return index / length
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Indexed) return false

            return index == other.index
        }

        override fun hashCode() = index
        override fun toString() = "Indexed(index=$index)"
    }

}

fun slot(x: @Range(from = 0, to = 8) Int, y: @Range(from = 0, to = 5) Int) = Slot.fromXY(x, y)
fun slot(index: Int) = Slot.fromIndex(index)