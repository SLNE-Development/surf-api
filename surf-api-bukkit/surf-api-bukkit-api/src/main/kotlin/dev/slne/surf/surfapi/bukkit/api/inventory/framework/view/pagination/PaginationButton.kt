package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.pagination

/**
 * Identifies the two navigation buttons in a paginated view.
 *
 * Each entry knows its column position and can calculate the linear slot index for a given
 * pagination button row.
 *
 * @property column the zero-based column index of the button (0–8)
 */
internal enum class PaginationButton(val column: Int) {
    /** The "previous page" (back) button, located in column 2. */
    LEFT(2),

    /** The "next page" (forward) button, located in column 6. */
    RIGHT(6);

    /**
     * Calculates the linear inventory slot index for this button in the given [paginationRow].
     *
     * @param paginationRow the 1-based row index where the button row is located
     * @return the linear slot index (0-based, left-to-right, top-to-bottom)
     */
    fun clickSlot(paginationRow: Int): Int {
        val effectiveRow = paginationRow - 1
        return column + effectiveRow * 9
    }
}