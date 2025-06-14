package dev.slne.surf.surfapi.bukkit.server.impl.inventory.pane.utils

import dev.slne.surf.surfapi.bukkit.api.inventory.pane.utils.Mask
import java.util.*
import kotlin.math.min

class MaskImpl(private val mask: Array<BooleanArray>) : Mask {

    override val enabledSlots: Int get() = amountOfEnabledSlots()
    override val length: Int get() = mask[0].size
    override val height: Int get() = mask.size

    constructor(vararg mask: String) : this(maskByString(*mask))

    companion object {
        private fun maskByString(vararg mask: String): Array<BooleanArray> {
            val maskArray = Array(mask.size) {
                BooleanArray(if (mask.isEmpty()) 0 else mask[0].length)
            }

            for (row in mask.indices) {
                val length = mask[row].length

                require(length == maskArray[row].size) { "Lengths of each string should be equal" }

                for (column in 0..<length) {
                    val character = mask[row].get(column)

                    when (character) {
                        '0' -> {
                            maskArray[row][column] = false
                        }

                        '1' -> {
                            maskArray[row][column] = true
                        }

                        else -> {
                            throw IllegalArgumentException("Strings may only contain '0' and '1'")
                        }
                    }
                }
            }

            return maskArray
        }

    }

    override fun setHeight(height: Int): MaskImpl {
        val newRows = Array(height) { BooleanArray(this.length) }

        for (index in 0..<min(height, this.height)) {
            System.arraycopy(mask[index], 0, newRows[index], 0, mask[index].size)
        }

        for (index in min(height, this.height)..<height) {
            newRows[index] = BooleanArray(this.length)

            Arrays.fill(newRows[index], true)
        }

        return MaskImpl(newRows)
    }

    override fun setLength(length: Int): MaskImpl {
        val newRows = Array(this.height) { BooleanArray(length) }

        for (index in mask.indices) {
            val newRow = BooleanArray(length)

            System.arraycopy(mask[index], 0, newRow, 0, min(length, mask[index].size))

            Arrays.fill(newRow, min(length, mask[index].size), newRow.size, true)

            newRows[index] = newRow
        }

        return MaskImpl(newRows)
    }

    fun amountOfEnabledSlots(): Int {
        var amount = 0

        for (row in mask) {
            for (cell in row) {
                if (cell) {
                    amount++
                }
            }
        }

        return amount
    }

    override fun getColumn(index: Int): BooleanArray {
        val column = BooleanArray(mask.size)

        for (i in 0 until height) {
            column[i] = mask[i][index]
        }

        return column
    }

    override fun getRow(index: Int): BooleanArray {
        val row = mask[index]

        return row.copyOf(row.size)
    }

    override fun isEnabled(x: Int, y: Int) = mask[y][x]

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MaskImpl) return false

        return this.mask.contentDeepEquals(other.mask)
    }

    override fun hashCode() = mask.contentDeepHashCode()

    override fun toString(): String {
        return "Mask{" +
                "mask=" + mask.contentDeepToString() +
                '}'
    }
}