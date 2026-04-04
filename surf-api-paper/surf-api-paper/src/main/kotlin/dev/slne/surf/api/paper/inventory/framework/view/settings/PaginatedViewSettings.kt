package dev.slne.surf.api.paper.inventory.framework.view.settings

import dev.slne.surf.api.paper.inventory.framework.view.settings.align.TextAlignment
import net.kyori.adventure.key.Key

/**
 * View settings for a paginated [AbstractPaginatedSurfView].
 *
 * Extends [SurfViewSettings] with pagination-specific configuration:
 * - The number of visible content rows and the total inventory rows via [paginationViewRows]
 * - The position (top or bottom) of the previous/next navigation buttons via [paginationButtonPosition]
 *
 * Create instances via the DSL builder [paginatedViewSettings] or through the
 * [PaginatedViewSettingsBuilder] within a `settings { }` DSL block.
 *
 * @property font the Adventure [Key] of the font used for the inventory title
 * @property headerTextAlignment horizontal alignment of the title text in the header
 * @property cancelOnClick whether inventory click events should be cancelled by default
 * @property cancelOnDrag whether inventory drag events should be cancelled by default
 * @property cancelOnDrop whether item-drop events should be cancelled by default
 * @property cancelOnPickup whether item-pickup events should be cancelled by default
 * @property navigateBackOnOutsideClick whether an outside click navigates to the parent view
 * @property paginationViewRows the [PaginationViewRows] controlling the number of content rows
 * @property paginationButtonPosition the [PaginationButtonPosition] for navigation buttons
 * @see SurfViewSettings
 * @see PaginationViewRows
 * @see PaginationButtonPosition
 */
data class PaginatedViewSettings(
    override val font: Key = SurfViewSettingsDefaults.DEFAULT_HEADER_FONT,
    override val headerTextAlignment: TextAlignment = SurfViewSettingsDefaults.DEFAULT_HEADER_ALIGNMENT,
    override val cancelOnClick: Boolean = SurfViewSettingsDefaults.DEFAULT_CANCEL_ON_CLICK,
    override val cancelOnDrag: Boolean = SurfViewSettingsDefaults.DEFAULT_CANCEL_ON_DRAG,
    override val cancelOnDrop: Boolean = SurfViewSettingsDefaults.DEFAULT_CANCEL_ON_DROP,
    override val cancelOnPickup: Boolean = SurfViewSettingsDefaults.DEFAULT_CANCEL_ON_PICKUP,
    override val navigateBackOnOutsideClick: Boolean = SurfViewSettingsDefaults.DEFAULT_NAVIGATE_BACK_ON_CLOSE,
    val paginationViewRows: PaginationViewRows = SurfViewSettingsDefaults.DEFAULT_PAGINATION_VIEW_ROWS,
    val paginationButtonPosition: PaginationButtonPosition = SurfViewSettingsDefaults.DEFAULT_PAGINATION_BUTTON_POSITION
) : SurfViewSettings {
    override val rows: ViewRows = paginationViewRows.actualRows
    internal val paginationButtonRow =
        if (paginationButtonPosition == PaginationButtonPosition.BOTTOM) {
            rows.rows
        } else {
            1
        }
}