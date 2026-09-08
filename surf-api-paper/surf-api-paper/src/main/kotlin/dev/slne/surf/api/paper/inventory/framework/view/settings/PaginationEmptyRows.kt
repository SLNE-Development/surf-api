package dev.slne.surf.api.paper.inventory.framework.view.settings

/**
 * Defines how many rows of a paginated view are left empty instead of being filled by the
 * pagination engine.
 *
 * The empty rows sit at the inventory edge opposite the navigation buttons, so they pad the
 * pagination content away from that edge: with the buttons at the bottom
 * ([PaginationButtonPosition.BOTTOM]) they are the topmost rows, with the buttons at the top
 * ([PaginationButtonPosition.TOP]) they are the bottommost ones.
 *
 * The empty rows are taken out of the content rows - they do not enlarge the inventory. The total
 * inventory height stays [PaginationViewRows.actualRows], so a view always keeps at least one
 * content row: [PaginatedViewSettings] rejects a combination that would leave none.
 *
 * - [NONE] - no empty row; the content starts right at the inventory edge
 * - [ONE] - 1 empty row
 * - [TWO] - 2 empty rows
 * - [THREE] - 3 empty rows
 *
 * @property rows the number of empty rows (0-3)
 * @see PaginatedViewSettings
 * @see PaginationViewRows
 */
enum class PaginationEmptyRows(val rows: Int) {
    /** No empty row; the pagination content starts right at the inventory edge. */
    NONE(0),

    /** 1 empty row between the pagination content and the inventory edge. */
    ONE(1),

    /** 2 empty rows between the pagination content and the inventory edge. */
    TWO(2),

    /** 3 empty rows between the pagination content and the inventory edge. */
    THREE(3);

    companion object {
        /**
         * Returns the [PaginationEmptyRows] for the given [rows] count, or `null` if it is not in
         * the range 0..3.
         *
         * @param rows the number of empty rows to look up (0..3)
         * @return the matching [PaginationEmptyRows], or `null`
         */
        fun byRowsOrNull(rows: Int): PaginationEmptyRows? = entries.getOrNull(rows)

        /**
         * Returns the [PaginationEmptyRows] for the given [rows] count.
         *
         * @param rows the number of empty rows to look up (0..3)
         * @return the matching [PaginationEmptyRows]
         * @throws IllegalArgumentException if [rows] is not in the range 0..3
         */
        fun byRows(rows: Int): PaginationEmptyRows = requireNotNull(byRowsOrNull(rows)) {
            "A paginated view supports 0 to 3 empty rows, but $rows were requested"
        }
    }
}
