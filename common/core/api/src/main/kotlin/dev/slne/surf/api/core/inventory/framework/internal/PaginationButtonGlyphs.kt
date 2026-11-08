package dev.slne.surf.api.core.inventory.framework.internal

import dev.slne.surf.api.shared.api.util.InternalSurfApi

/**
 * Shared per-row glyph table for the pagination button overlay rendered in the inventory header.
 *
 * There are four visual states depending on whether the left and right navigation buttons are
 * available. Each entry owns the six glyph characters (one per inventory row) for its state, so
 * the two platform `PaginationButtonGlyphComponent` hierarchies cannot drift apart.
 *
 * This is internal infrastructure — use the platform `PaginationButtonGlyphComponent` instead.
 */
@InternalSurfApi
enum class PaginationButtonGlyphs {

    /** Both navigation buttons are disabled (no previous and no next page). */
    DISABLED {
        override fun glyph(rows: Int): Char = when (rows) {
            1 -> 'ꐕ'
            2 -> 'ꐙ'
            3 -> 'ꐝ'
            4 -> 'ꐡ'
            5 -> 'ꐥ'
            6 -> 'ꐩ'
            else -> error("Invalid row: $rows")
        }
    },

    /** The right (next) button is disabled; only the left (previous) button is active. */
    DISABLED_RIGHT {
        override fun glyph(rows: Int): Char = when (rows) {
            1 -> 'ꐖ'
            2 -> 'ꐚ'
            3 -> 'ꐞ'
            4 -> 'ꐢ'
            5 -> 'ꐦ'
            6 -> 'ꐪ'
            else -> error("Invalid row: $rows")
        }
    },

    /** The left (previous) button is disabled; only the right (next) button is active. */
    DISABLED_LEFT {
        override fun glyph(rows: Int): Char = when (rows) {
            1 -> 'ꐗ'
            2 -> 'ꐛ'
            3 -> 'ꐟ'
            4 -> 'ꐣ'
            5 -> 'ꐧ'
            6 -> 'ꐫ'
            else -> error("Invalid row: $rows")
        }
    },

    /** Both navigation buttons are active (there are previous and next pages). */
    ENABLED {
        override fun glyph(rows: Int): Char = when (rows) {
            1 -> 'ꐘ'
            2 -> 'ꐜ'
            3 -> 'ꐠ'
            4 -> 'ꐤ'
            5 -> 'ꐨ'
            6 -> 'ꐬ'
            else -> error("Invalid row: $rows")
        }
    };

    /**
     * Returns the glyph character for this button state at the given [rows] (1-based row index).
     *
     * @param rows the 1-based row number of the button row
     * @return the corresponding Unicode glyph character
     * @throws IllegalStateException if [rows] is outside the valid range 1..6
     */
    abstract fun glyph(rows: Int): Char
}