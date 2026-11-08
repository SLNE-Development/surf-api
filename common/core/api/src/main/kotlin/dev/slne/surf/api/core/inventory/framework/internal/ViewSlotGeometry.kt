package dev.slne.surf.api.core.inventory.framework.internal

import dev.slne.surf.api.shared.api.util.InternalSurfApi

/**
 * Shared pixel math for addressing the inventory's slot grid from the header.
 *
 * Everything the header draws is a glyph inside the inventory title, so the only thing that can be
 * computed at runtime is the **horizontal** offset of a glyph: how far right of the title origin a
 * given slot column starts. The **vertical** position of a glyph is baked into the `ascent` of its
 * font provider in the resource pack and cannot be changed by the server, which is why a texture
 * that has to appear in a specific slot row needs its own glyph (see the platform
 * `ViewBlockCellComponent`) and text that has to appear centred in a specific slot row needs its
 * own font (see the platform `SurfViewSettingsDefaults.rowFont`).
 *
 * This is internal infrastructure — use the platform header DSL instead.
 */
@InternalSurfApi
object ViewSlotGeometry {

    /** Number of slot columns in a chest-type inventory. */
    const val COLUMNS = 9

    /** Maximum number of slot rows in a chest-type inventory. */
    const val MAX_ROWS = 6

    /** Horizontal pitch of the slot grid in pixels: one slot plus its one-pixel border. */
    const val SLOT_SIZE = 18

    /**
     * Returns the pixel offset of the left edge of [column] relative to the title origin.
     *
     * @param column the zero-based column index
     * @param originX the pixel offset of column `0`
     * @param slotSize the horizontal pitch of the slot grid
     */
    fun columnShift(column: Int, originX: Int, slotSize: Int = SLOT_SIZE): Int =
        originX + (column * slotSize)

    /**
     * Returns the pixel width spanned by [columns] adjacent slot columns.
     *
     * @param columns how many columns the span covers
     * @param slotSize the horizontal pitch of the slot grid
     */
    fun spanWidth(columns: Int, slotSize: Int = SLOT_SIZE): Int = columns * slotSize

    /**
     * Returns the zero-based column index of the slot at [slot].
     *
     * @param slot the zero-based slot index, counted left-to-right then top-to-bottom
     * @param columns the number of columns per row
     */
    fun columnOf(slot: Int, columns: Int = COLUMNS): Int = Math.floorMod(slot, columns)

    /**
     * Returns the **one-based** row index of the slot at [slot], matching the row numbering the
     * per-row block glyphs and row fonts use.
     *
     * @param slot the zero-based slot index, counted left-to-right then top-to-bottom
     * @param columns the number of columns per row
     */
    fun rowOf(slot: Int, columns: Int = COLUMNS): Int = Math.floorDiv(slot, columns) + 1

    /**
     * Returns the zero-based slot index of the cell at [column] and one-based [row].
     *
     * @param column the zero-based column index
     * @param row the one-based row index
     * @param columns the number of columns per row
     */
    fun slotOf(column: Int, row: Int, columns: Int = COLUMNS): Int =
        ((row - 1) * columns) + column
}
