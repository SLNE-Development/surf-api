package dev.slne.surf.api.paper.inventory.framework.view.settings.builder

import dev.slne.surf.api.paper.inventory.framework.view.InventoryFrameworkDSL
import dev.slne.surf.api.paper.inventory.framework.view.settings.PaginatedViewSettings
import dev.slne.surf.api.paper.inventory.framework.view.settings.PaginationButtonPosition
import dev.slne.surf.api.paper.inventory.framework.view.settings.PaginationViewRows
import dev.slne.surf.api.paper.inventory.framework.view.settings.SurfViewSettingsDefaults

/**
 * DSL builder for [PaginatedViewSettings].
 *
 * Extends [SurfViewSettingsBuilder] with pagination-specific properties:
 * - [paginationViewRows] — controls the number of visible item rows and total inventory height
 * - [paginationButtonPosition] — controls whether the prev/next buttons are at the top or bottom
 *
 * Create instances via [paginatedViewSettings] or the `settings { }` DSL function in a
 * [paginatedSurfView][dev.slne.surf.api.paper.api.inventory.framework.view.paginatedSurfView] block.
 *
 * ```kotlin
 * paginatedSurfView("Items") {
 *     settings {
 *         paginationViewRows(PaginationViewRows.THREE)
 *         paginationButtonsAtBottom()
 *         cancelAllInteractions()
 *     }
 * }
 * ```
 *
 * @see paginatedViewSettings
 * @see SurfViewSettingsBuilder
 * @see PaginatedViewSettings
 */
@InventoryFrameworkDSL
class PaginatedViewSettingsBuilder @PublishedApi internal constructor() :
    SurfViewSettingsBuilder() {

    /**
     * The [PaginationViewRows] controlling the number of content rows and total inventory height.
     * Defaults to [SurfViewSettingsDefaults.DEFAULT_PAGINATION_VIEW_ROWS].
     */
    var paginationViewRows: PaginationViewRows =
        SurfViewSettingsDefaults.DEFAULT_PAGINATION_VIEW_ROWS
        private set

    /**
     * Sets the [PaginationViewRows].
     *
     * @param rows the desired [PaginationViewRows]
     */
    fun paginationViewRows(rows: PaginationViewRows) {
        this.paginationViewRows = rows
    }

    /**
     * The [PaginationButtonPosition] controlling whether navigation buttons are at the top or bottom.
     * Defaults to [SurfViewSettingsDefaults.DEFAULT_PAGINATION_BUTTON_POSITION].
     */
    var paginationButtonPosition: PaginationButtonPosition =
        SurfViewSettingsDefaults.DEFAULT_PAGINATION_BUTTON_POSITION
        private set

    /**
     * Sets the [PaginationButtonPosition].
     *
     * @param position the desired [PaginationButtonPosition]
     */
    fun paginationButtonPosition(position: PaginationButtonPosition) {
        this.paginationButtonPosition = position
    }

    /** Shorthand for `paginationButtonPosition(PaginationButtonPosition.BOTTOM)`. */
    fun paginationButtonsAtBottom() {
        paginationButtonPosition(PaginationButtonPosition.BOTTOM)
    }

    /** Shorthand for `paginationButtonPosition(PaginationButtonPosition.TOP)`. */
    fun paginationButtonsAtTop() {
        paginationButtonPosition(PaginationButtonPosition.TOP)
    }

    @PublishedApi
    override fun build(): PaginatedViewSettings = PaginatedViewSettings(
        font = font,
        headerTextAlignment = headerTextAlignment,
        cancelOnClick = cancelOnClick,
        cancelOnDrag = cancelOnDrag,
        cancelOnDrop = cancelOnDrop,
        cancelOnPickup = cancelOnPickup,
        navigateBackOnOutsideClick = navigateBackOnOutsideClick,
        paginationViewRows = paginationViewRows,
        paginationButtonPosition = paginationButtonPosition,
    )
}

/**
 * Creates a [PaginatedViewSettings] instance using a [PaginatedViewSettingsBuilder] DSL block.
 *
 * ```kotlin
 * val settings = paginatedViewSettings {
 *     paginationViewRows(PaginationViewRows.THREE)
 *     paginationButtonsAtBottom()
 * }
 * ```
 *
 * @param block configuration block applied to a [PaginatedViewSettingsBuilder]
 * @return the built [PaginatedViewSettings]
 */
inline fun paginatedViewSettings(block: PaginatedViewSettingsBuilder.() -> Unit): PaginatedViewSettings =
    PaginatedViewSettingsBuilder().apply(block).build()