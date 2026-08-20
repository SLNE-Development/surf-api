package dev.slne.surf.api.minestom.inventory.framework.view.settings.builder

import dev.slne.surf.api.minestom.inventory.framework.view.InventoryFrameworkDSL
import dev.slne.surf.api.minestom.inventory.framework.view.pagination.PaginationPageIndicator
import dev.slne.surf.api.minestom.inventory.framework.view.settings.PaginatedViewSettings
import dev.slne.surf.api.minestom.inventory.framework.view.settings.PaginationButtonPosition
import dev.slne.surf.api.minestom.inventory.framework.view.settings.PaginationViewRows
import dev.slne.surf.api.minestom.inventory.framework.view.settings.SurfViewSettingsDefaults
import net.kyori.adventure.sound.Sound

/**
 * DSL builder for [PaginatedViewSettings].
 *
 * Extends [SurfViewSettingsBuilder] with pagination-specific properties:
 * - [paginationViewRows] — controls the number of visible item rows and total inventory height
 * - [paginationButtonPosition] — controls whether the prev/next buttons are at the top or bottom
 *
 * Create instances via [paginatedViewSettings] or the `settings { }` DSL function in a
 * [paginatedSurfView][dev.slne.surf.api.minestom.api.inventory.framework.view.paginatedSurfView] block.
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

    /**
     * Renders the page counter between the navigation buttons, or `null` to render none.
     * Defaults to [SurfViewSettingsDefaults.DEFAULT_PAGINATION_PAGE_INDICATOR], which renders
     * `current/total`.
     */
    var paginationPageIndicator: PaginationPageIndicator? =
        SurfViewSettingsDefaults.DEFAULT_PAGINATION_PAGE_INDICATOR
        private set

    /**
     * Sets the [PaginationPageIndicator] that renders the page counter.
     *
     * ```kotlin
     * settings {
     *     paginationPageIndicator { current, total -> text("Seite $current von $total") }
     * }
     * ```
     *
     * @param indicator renders the counter, or `null` to render none
     */
    fun paginationPageIndicator(indicator: PaginationPageIndicator?) {
        this.paginationPageIndicator = indicator
    }

    /** Renders no page counter between the navigation buttons. */
    fun noPaginationPageIndicator() {
        paginationPageIndicator(null)
    }

    /**
     * The [Sound] played to the viewer when a navigation button switches to another page, or `null`
     * to play none. Defaults to [SurfViewSettingsDefaults.DEFAULT_PAGINATION_SWITCH_SOUND], the
     * vanilla book page-turn sound.
     */
    var paginationSwitchSound: Sound? =
        SurfViewSettingsDefaults.DEFAULT_PAGINATION_SWITCH_SOUND
        private set

    /**
     * Sets the [Sound] played when a navigation button switches to another page.
     *
     * @param sound the sound to play, or `null` to play none
     */
    fun paginationSwitchSound(sound: Sound?) {
        this.paginationSwitchSound = sound
    }

    /** Plays no sound when a navigation button switches to another page. */
    fun noPaginationSwitchSound() {
        paginationSwitchSound(null)
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
        headerTextColor = headerTextColor,
        headerFontMetrics = headerFontMetrics,
        rowFontMetrics = rowFontMetrics,
        headerGeometry = headerGeometry,
        backgroundGlyph = backgroundGlyph,
        rowFonts = rowFonts,
        cancelOnClick = cancelOnClick,
        cancelOnDrag = cancelOnDrag,
        cancelOnDrop = cancelOnDrop,
        cancelOnPickup = cancelOnPickup,
        navigateBackOnOutsideClick = navigateBackOnOutsideClick,
        paginationViewRows = paginationViewRows,
        paginationButtonPosition = paginationButtonPosition,
        paginationPageIndicator = paginationPageIndicator,
        paginationSwitchSound = paginationSwitchSound,
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