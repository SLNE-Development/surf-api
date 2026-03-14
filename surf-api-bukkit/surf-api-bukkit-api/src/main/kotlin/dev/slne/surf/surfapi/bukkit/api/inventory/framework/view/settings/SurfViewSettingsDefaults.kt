package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.align.TextAlignment
import dev.slne.surf.surfapi.core.api.messages.adventure.key

/**
 * Centralised default values for all [SurfViewSettings] properties.
 *
 * These constants and properties are referenced by [SimpleViewSettings],
 * [PaginatedViewSettings], and the corresponding builder classes to ensure
 * consistent defaults across all view types.
 *
 * All defaults can be overridden per-view via the `settings { }` DSL block.
 *
 * @see SurfViewSettings
 * @see dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.builder.SurfViewSettingsBuilder
 */
object SurfViewSettingsDefaults {
    /** Default value for [SurfViewSettings.cancelOnClick]: `true`. */
    const val DEFAULT_CANCEL_ON_CLICK = true
    /** Default value for [SurfViewSettings.cancelOnDrag]: `true`. */
    const val DEFAULT_CANCEL_ON_DRAG = true
    /** Default value for [SurfViewSettings.cancelOnDrop]: `true`. */
    const val DEFAULT_CANCEL_ON_DROP = true
    /** Default value for [SurfViewSettings.cancelOnPickup]: `true`. */
    const val DEFAULT_CANCEL_ON_PICKUP = true
    /** Default value for [SurfViewSettings.navigateBackOnOutsideClick]: `true`. */
    const val DEFAULT_NAVIGATE_BACK_ON_CLOSE = true

    /** The Adventure [Key] for the default header font (`surf:menu_font_normal_ascended`). */
    val DEFAULT_HEADER_FONT = key("surf", "menu_font_normal_ascended")

    /** The Adventure [Key] for the default menu font (`surf:menu`), used for the title component. */
    val DEFAULT_MENU_FONT = key("surf", "menu")

    /** Default [TextAlignment] for the header: [TextAlignment.CENTER]. */
    val DEFAULT_HEADER_ALIGNMENT = TextAlignment.CENTER

    /** Default number of rows for simple views: [ViewRows.FIVE]. */
    val DEFAULT_VIEW_ROWS = ViewRows.FIVE

    /** Default [PaginationViewRows] for paginated views: [PaginationViewRows.FOUR]. */
    val DEFAULT_PAGINATION_VIEW_ROWS = PaginationViewRows.FOUR

    /** Default [PaginationButtonPosition] for paginated views: [PaginationButtonPosition.BOTTOM]. */
    val DEFAULT_PAGINATION_BUTTON_POSITION = PaginationButtonPosition.BOTTOM
}