package dev.slne.surf.api.inventory.framework.view.settings

/**
 * The position of the pagination navigation buttons (previous / next page) within the inventory.
 *
 * - [TOP] — buttons appear in the first row (row index 0)
 * - [BOTTOM] — buttons appear in the last row of the view
 *
 * Set through the view settings' `paginationButtonPosition`.
 */
enum class PaginationButtonPosition {
    /** Navigation buttons appear in the top row of the inventory. */
    TOP,

    /** Navigation buttons appear in the bottom row of the inventory. */
    BOTTOM
}