package dev.slne.surf.surfapi.bukkit.server.impl.inventory.pane.utils

import dev.slne.surf.surfapi.bukkit.api.inventory.pane.utils.Pattern
import dev.slne.surf.surfapi.core.api.util.mutableIntListOf


class PatternImpl(private val pattern: Array<IntArray>) : Pattern {
    constructor(vararg pattern: String) : this(initializePattern(*pattern))

    override val length: Int
        get() = pattern[0].size

    override val height: Int
        get() = pattern.size

    override fun setHeight(height: Int): PatternImpl {
        require(height > 0) { "Height must be greater than 0" }

        val newRows = Array(height) { IntArray(length) }

        for (index in 0 until minOf(height, this.height)) {
            System.arraycopy(pattern[index], 0, newRows[index], 0, pattern[index].size)
        }

        for (index in minOf(height, this.height) until height) {
            val previousRow = newRows[index - 1]

            newRows[index] = previousRow.copyOf(previousRow.size)
        }

        return PatternImpl(newRows)
    }

    override fun setLength(length: Int): PatternImpl {
        require(length > 0) { "Length must be greater than 0" }

        val newRows = Array(height) { IntArray(length) }

        for (index in 0 until height) {
            val newRow = IntArray(length)
            val row = pattern[index]
            val minLength = minOf(length, row.size)

            System.arraycopy(row, 0, newRow, 0, minLength)

            for (column in minLength until length) {
                newRow[column] = newRow[minLength - 1]
            }

            newRows[index] = newRow
        }

        return PatternImpl(newRows)
    }

    override fun getColumn(index: Int): IntArray {
        if (index < 0 || index >= length) {
            throw IndexOutOfBoundsException("Column index $index is out of bounds for pattern of length $length")
        }

        val column = IntArray(height)

        for (i in 0 until height) {
            column[i] = pattern[i][index]
        }

        return column
    }

    override fun contains(char: Char): Boolean {
        for (row in pattern) {
            for (cell in row) {
                if (cell != char.code) {
                    continue
                }

                return true
            }
        }

        return false
    }

    override fun getRow(index: Int): IntArray {
        if (index < 0 || index >= height) {
            throw IndexOutOfBoundsException("Row index $index is out of bounds for pattern of height $height")
        }

        val row = pattern[index]

        return row.copyOf(row.size)
    }

    override fun getChar(x: Int, y: Int): Char {
        if (x < 0 || x >= length || y < 0 || y >= height) {
            throw IndexOutOfBoundsException("Coordinates ($x, $y) are out of bounds for pattern of size ${length}x${height}")
        }

        return pattern[y][x].toChar()
    }

    override fun hashCode() = pattern.contentDeepHashCode()

    override fun toString() = buildString {
        append("Pattern{")
        append("pattern=")
        append(pattern.contentDeepToString())
        append('}')
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PatternImpl) return false

        return pattern.contentDeepEquals(other.pattern)
    }

    companion object {
        private fun initializePattern(vararg pattern: String): Array<IntArray> {
            val rows = pattern.size
            val zeroRows = rows == 0

            val patternArray = Array(rows) {
                IntArray(
                    if (zeroRows) 0 else pattern[0].codePointCount(
                        0,
                        pattern[0].length
                    )
                )
            }

            if (zeroRows) {
                return patternArray
            }

            val globalLength = pattern[0].length

            for (index in 0 until rows) {
                val row = pattern[index]
                val length = row.codePointCount(0, row.length)

                if (length != globalLength) {
                    throw IllegalArgumentException("All rows must have the same length. Row $index has length $length, expected $globalLength")
                }

                val values = mutableIntListOf()
                row.codePoints().forEach(values::add)

                for (column in 0 until values.size) {
                    patternArray[index][column] = values.getInt(column)
                }
            }

            return patternArray
        }
    }
}