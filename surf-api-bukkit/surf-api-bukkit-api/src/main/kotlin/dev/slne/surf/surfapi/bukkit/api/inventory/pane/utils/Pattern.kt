package dev.slne.surf.surfapi.bukkit.api.inventory.pane.utils

import dev.slne.surf.surfapi.core.api.util.mutableIntListOf

internal class Pattern(
    private val pattern: Array<IntArray>,
) {
    constructor(vararg pattern: String) : this(initializePattern(*pattern))

    fun getLength() = pattern[0].size
    fun getHeight() = pattern.size

    fun setHeight(height: Int): Pattern {
        if (height <= 0) {
            throw IllegalArgumentException("Height must be greater than 0")
        }

        val newRows = Array(height) { IntArray(getLength()) }

        for (index in 0 until minOf(height, getHeight())) {
            System.arraycopy(pattern[index], 0, newRows[index], 0, pattern[index].size)
        }

        for (index in minOf(height, getHeight()) until height) {
            val previousRow = newRows[index - 1]

            newRows[index] = previousRow.copyOf(previousRow.size)
        }

        return Pattern(newRows)
    }

    fun setLength(length: Int): Pattern {
        if (length <= 0) {
            throw IllegalArgumentException("Length must be greater than 0")
        }

        val newRows = Array(getHeight()) { IntArray(length) }

        for (index in 0 until getHeight()) {
            val newRow = IntArray(length)
            val row = pattern[index]
            val minLength = minOf(length, row.size)

            System.arraycopy(row, 0, newRow, 0, minLength)

            for (column in minLength until length) {
                newRow[column] = newRow[minLength - 1]
            }

            newRows[index] = newRow
        }

        return Pattern(newRows)
    }

    fun getColumn(index: Int): IntArray {
        if (index < 0 || index >= getLength()) {
            throw IndexOutOfBoundsException("Column index $index is out of bounds for pattern of length ${getLength()}")
        }

        val column = IntArray(getHeight())

        for (i in 0 until getHeight()) {
            column[i] = pattern[i][index]
        }

        return column
    }

    fun contains(character: Int): Boolean {
        for (row in pattern) {
            for (cell in row) {
                if (cell != character) {
                    continue
                }

                return true
            }
        }

        return false
    }

    fun getRow(index: Int): IntArray {
        if (index < 0 || index >= getHeight()) {
            throw IndexOutOfBoundsException("Row index $index is out of bounds for pattern of height ${getHeight()}")
        }

        val row = pattern[index]

        return row.copyOf(row.size)
    }

    fun getCharacter(x: Int, y: Int): Int {
        if (x < 0 || x >= getLength() || y < 0 || y >= getHeight()) {
            throw IndexOutOfBoundsException("Coordinates ($x, $y) are out of bounds for pattern of size ${getLength()}x${getHeight()}")
        }

        return pattern[y][x]
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
        if (other !is Pattern) return false

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