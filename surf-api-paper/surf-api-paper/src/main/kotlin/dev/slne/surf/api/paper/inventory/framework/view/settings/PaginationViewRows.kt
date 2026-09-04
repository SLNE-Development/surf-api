package dev.slne.surf.api.paper.inventory.framework.view.settings

/**
 * Defines the number of pagination content rows and the total inventory size for a paginated view.
 *
 * The total inventory has exactly one row more than the content rows: the row that carries the
 * navigation buttons, which sits above or below the content depending on
 * [PaginatedViewSettings.paginationButtonPosition]. Every other row is filled by the pagination
 * engine - the topmost one included, so no row is left empty just to keep the buttons apart from
 * the content. Set [PaginatedViewSettings.paginationEmptyRows] to keep one to three rows free
 * again; those rows are taken out of the content rows, the inventory height stays [actualRows].
 *
 * - [ONE] - 1 content row, 2 total rows ([ViewRows.TWO])
 * - [TWO] - 2 content rows, 3 total rows ([ViewRows.THREE])
 * - [THREE] - 3 content rows, 4 total rows ([ViewRows.FOUR])
 * - [FOUR] - 4 content rows, 5 total rows ([ViewRows.FIVE])
 * - [FIVE] - 5 content rows, 6 total rows ([ViewRows.SIX])
 *
 * @property actualRows the total [ViewRows] value: the content rows plus the button row
 * @see PaginatedViewSettings
 */
enum class PaginationViewRows(val actualRows: ViewRows) {
    /** 1 content row; 2 total inventory rows. */
    ONE(ViewRows.TWO),

    /** 2 content rows; 3 total inventory rows. */
    TWO(ViewRows.THREE),

    /** 3 content rows; 4 total inventory rows. */
    THREE(ViewRows.FOUR),

    /** 4 content rows; 5 total inventory rows. */
    FOUR(ViewRows.FIVE),

    /** 5 content rows; 6 total inventory rows. */
    FIVE(ViewRows.SIX);

    /**
     * How many rows the pagination engine fills at most: one less than [actualRows], because a
     * single row is reserved for the navigation buttons. Configured empty rows reduce it further,
     * see [PaginatedViewSettings.paginationContentRowCount].
     */
    val contentRows: Int = actualRows.rows - 1

    /** @suppress */
    @Deprecated(
        "The content rows depend on the button position now, use PaginatedViewSettings instead",
        level = DeprecationLevel.HIDDEN
    )
    val paginationContentRows: IntRange
        get() = 1..contentRows
}
