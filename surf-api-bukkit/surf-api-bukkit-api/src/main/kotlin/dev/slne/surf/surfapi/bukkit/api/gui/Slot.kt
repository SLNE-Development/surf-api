package dev.slne.surf.surfapi.bukkit.api.gui

/**
 * Represents a slot in a GUI inventory.
 * Can be created from either a linear index or x,y coordinates.
 */
data class Slot(val index: Int) {
    
    /**
     * Row (y coordinate, 0-based).
     */
    val row: Int
        get() = index / 9
    
    /**
     * Column (x coordinate, 0-based).
     */
    val column: Int
        get() = index % 9
    
    /**
     * Create a slot from x,y coordinates.
     */
    constructor(column: Int, row: Int) : this(row * 9 + column)
    
    /**
     * Convert to coordinates (column, row).
     */
    fun toCoordinates(): Pair<Int, Int> = column to row
    
    companion object {
        /**
         * Create a slot from coordinates.
         */
        fun at(column: Int, row: Int): Slot = Slot(column, row)
        
        /**
         * Create a slot from an index.
         */
        fun of(index: Int): Slot = Slot(index)
    }
    
    override fun toString(): String = "Slot(index=$index, column=$column, row=$row)"
}
