package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings

/**
 * The position of the pagination navigation buttons (previous / next page) within the inventory.
 *
 * - [TOP] — buttons appear in the first row (row index 0)
 * - [BOTTOM] — buttons appear in the last row of the view
 *
 * @see PaginatedViewSettings
 * @see dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.builder.PaginatedViewSettingsBuilder.paginationButtonPosition
 */
enum class PaginationButtonPosition {
    /** Navigation buttons appear in the top row of the inventory. */
    TOP,

    /** Navigation buttons appear in the bottom row of the inventory. */
    BOTTOM
}