package dev.slne.surf.api.paper.inventory.framework.view.settings

import dev.slne.surf.api.paper.inventory.framework.view.settings.align.TextAlignment
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.TextColor

/**
 * Sealed interface that defines the full configuration contract for all Surf view types.
 *
 * Concrete implementations are [SimpleViewSettings] (for non-paginated views) and
 * [PaginatedViewSettings] (for paginated views). All properties have sensible defaults
 * defined in [SurfViewSettingsDefaults].
 *
 * @property font the Adventure [Key] of the font for the inventory title; the client's own font
 *   by default
 * @property headerTextAlignment horizontal [TextAlignment] of the title in the header
 * @property headerTextColor colour for header text that does not carry one of its own
 * @property headerFontMetrics glyph metrics of the font the title is rendered in
 * @property rowFontMetrics glyph metrics of the fonts row text is rendered in
 * @property headerGeometry the pixel geometry of the inventory and its slot grid
 * @property backgroundGlyph whether the per-row background glyph of a custom inventory texture is
 *   rendered
 * @property rowFonts the fonts that render header text on a slot row, keyed by one-based row
 * @property cancelOnClick whether inventory click events are cancelled by default
 * @property cancelOnDrag whether inventory drag events are cancelled by default
 * @property cancelOnDrop whether item-drop events are cancelled by default
 * @property cancelOnPickup whether item-pickup events are cancelled by default
 * @property navigateBackOnOutsideClick whether clicking outside the inventory navigates
 *   back to the previous (parent) view; nothing is drawn in the header for it
 * @property rows the number of chest rows displayed in the inventory
 * @see SimpleViewSettings
 * @see PaginatedViewSettings
 * @see SurfViewSettingsDefaults
 */
sealed interface SurfViewSettings {
    val font: Key
    val headerTextAlignment: TextAlignment
    val headerTextColor: TextColor
    val headerFontMetrics: ViewFontMetrics
    val rowFontMetrics: ViewFontMetrics
    val headerGeometry: ViewHeaderGeometry
    val backgroundGlyph: Boolean
    val rowFonts: Int2ObjectMap<Key>
    val cancelOnClick: Boolean
    val cancelOnDrag: Boolean
    val cancelOnDrop: Boolean
    val cancelOnPickup: Boolean
    val navigateBackOnOutsideClick: Boolean
    val rows: ViewRows

    /**
     * Returns the font that renders header text on the one-based slot [row].
     *
     * Falls back to [SurfViewSettingsDefaults.rowFont] when [rowFonts] carries no entry for [row].
     *
     * @param row the one-based slot row (1-6)
     */
    fun rowFont(@ViewRows.Companion.Rows row: Int): Key =
        rowFonts[row] ?: SurfViewSettingsDefaults.rowFont(row)
}