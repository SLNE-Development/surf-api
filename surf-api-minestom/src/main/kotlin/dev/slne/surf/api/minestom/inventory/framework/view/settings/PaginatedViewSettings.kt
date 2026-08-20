package dev.slne.surf.api.minestom.inventory.framework.view.settings

import dev.slne.surf.api.minestom.inventory.framework.view.pagination.PaginationPageIndicator
import dev.slne.surf.api.minestom.inventory.framework.view.settings.align.TextAlignment
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.format.TextColor

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
 * @property headerTextAlignment horizontal alignment of the title text
 * @property headerTextColor colour for header text that does not carry one of its own
 * @property headerFontMetrics glyph metrics of the font the title is rendered in
 * @property rowFontMetrics glyph metrics of the fonts row text is rendered in
 * @property headerGeometry the pixel geometry of the inventory and its slot grid
 * @property backgroundGlyph whether the per-row background glyph of a custom inventory texture is
 *   rendered
 * @property rowFonts the fonts that render header text on a slot row, keyed by one-based row in the header
 * @property cancelOnClick whether inventory click events should be cancelled by default
 * @property cancelOnDrag whether inventory drag events should be cancelled by default
 * @property cancelOnDrop whether item-drop events should be cancelled by default
 * @property cancelOnPickup whether item-pickup events should be cancelled by default
 * @property navigateBackOnOutsideClick whether an outside click navigates to the parent view
 * @property paginationViewRows the [PaginationViewRows] controlling the number of content rows
 * @property paginationButtonPosition the [PaginationButtonPosition] for navigation buttons
 * @property paginationPageIndicator renders the page counter between the navigation buttons, or
 *   `null` to render none
 * @property paginationSwitchSound the [Sound] played to the viewer when a navigation button
 *   switches to another page, or `null` to play none
 * @see SurfViewSettings
 * @see PaginationViewRows
 * @see PaginationButtonPosition
 */
data class PaginatedViewSettings(
    override val font: Key = SurfViewSettingsDefaults.DEFAULT_HEADER_FONT,
    override val headerTextAlignment: TextAlignment = SurfViewSettingsDefaults.DEFAULT_HEADER_ALIGNMENT,
    override val headerTextColor: TextColor = SurfViewSettingsDefaults.DEFAULT_HEADER_TEXT_COLOR,
    override val headerFontMetrics: ViewFontMetrics = SurfViewSettingsDefaults.DEFAULT_HEADER_FONT_METRICS,
    override val rowFontMetrics: ViewFontMetrics = SurfViewSettingsDefaults.DEFAULT_ROW_FONT_METRICS,
    override val headerGeometry: ViewHeaderGeometry = SurfViewSettingsDefaults.DEFAULT_HEADER_GEOMETRY,
    override val backgroundGlyph: Boolean = SurfViewSettingsDefaults.DEFAULT_BACKGROUND_GLYPH,
    override val rowFonts: Int2ObjectMap<Key> = SurfViewSettingsDefaults.DEFAULT_ROW_FONTS,
    override val cancelOnClick: Boolean = SurfViewSettingsDefaults.DEFAULT_CANCEL_ON_CLICK,
    override val cancelOnDrag: Boolean = SurfViewSettingsDefaults.DEFAULT_CANCEL_ON_DRAG,
    override val cancelOnDrop: Boolean = SurfViewSettingsDefaults.DEFAULT_CANCEL_ON_DROP,
    override val cancelOnPickup: Boolean = SurfViewSettingsDefaults.DEFAULT_CANCEL_ON_PICKUP,
    override val navigateBackOnOutsideClick: Boolean = SurfViewSettingsDefaults.DEFAULT_NAVIGATE_BACK_ON_CLOSE,
    val paginationViewRows: PaginationViewRows = SurfViewSettingsDefaults.DEFAULT_PAGINATION_VIEW_ROWS,
    val paginationButtonPosition: PaginationButtonPosition = SurfViewSettingsDefaults.DEFAULT_PAGINATION_BUTTON_POSITION,
    val paginationPageIndicator: PaginationPageIndicator? = SurfViewSettingsDefaults.DEFAULT_PAGINATION_PAGE_INDICATOR,
    val paginationSwitchSound: Sound? = SurfViewSettingsDefaults.DEFAULT_PAGINATION_SWITCH_SOUND,
) : SurfViewSettings {
    override val rows: ViewRows = paginationViewRows.actualRows
    internal val paginationButtonRow =
        if (paginationButtonPosition == PaginationButtonPosition.BOTTOM) {
            rows.rows
        } else {
            1
        }

    /**
     * The one-based rows the pagination engine fills: every row except [paginationButtonRow], so
     * the content starts right at the top edge of the inventory.
     */
    internal val paginationContentRows: IntRange =
        if (paginationButtonPosition == PaginationButtonPosition.BOTTOM) {
            1 until rows.rows
        } else {
            2..rows.rows
        }
}