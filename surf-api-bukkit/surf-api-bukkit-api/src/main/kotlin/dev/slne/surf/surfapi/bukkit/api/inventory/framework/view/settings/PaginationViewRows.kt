package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings

/**
 * Defines the number of pagination content rows and the total inventory size for a paginated view.
 *
 * The total inventory has two additional rows compared to the content rows: one top row and one
 * bottom row. The top and bottom rows are used for the inventory border/buttons, while the
 * content rows are populated by the pagination engine.
 *
 * - [ONE] — 1 content row, 3 total rows ([ViewRows.THREE])
 * - [TWO] — 2 content rows, 4 total rows ([ViewRows.FOUR])
 * - [THREE] — 3 content rows, 5 total rows ([ViewRows.FIVE])
 * - [FOUR] — 4 content rows, 6 total rows ([ViewRows.SIX])
 *
 * @property actualRows the total [ViewRows] value (including top and bottom rows)
 * @property paginationContentRows the range of 1-based row indices used for pagination content
 * @see PaginatedViewSettings
 */
enum class PaginationViewRows(val actualRows: ViewRows, val paginationContentRows: IntRange) {
    /** 1 content row; 3 total inventory rows. */
    ONE(ViewRows.THREE, 2..2),

    /** 2 content rows; 4 total inventory rows. */
    TWO(ViewRows.FOUR, 2..3),

    /** 3 content rows; 5 total inventory rows. */
    THREE(ViewRows.FIVE, 2..4),

    /** 4 content rows; 6 total inventory rows. */
    FOUR(ViewRows.SIX, 2..5);
}