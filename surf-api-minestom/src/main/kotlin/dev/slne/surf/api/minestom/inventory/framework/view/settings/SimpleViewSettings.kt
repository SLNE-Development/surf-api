package dev.slne.surf.api.minestom.inventory.framework.view.settings

import dev.slne.surf.api.minestom.inventory.framework.view.settings.align.TextAlignment
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.TextColor

/**
 * View settings for a simple (non-paginated) [AbstractSurfView].
 *
 * Create instances via the DSL builder [simpleViewSettings] or through
 * the [SimpleViewSettingsBuilder] within a `settings { }` DSL block.
 *
 * @property font the Adventure [Key] of the font used for the inventory title
 * @property headerTextAlignment horizontal alignment of the title text
 * @property headerTextColor colour for header text that does not carry one of its own
 * @property headerFontMetrics glyph metrics of the font the title is rendered in
 * @property rowFontMetrics glyph metrics of the fonts row text is rendered in
 * @property headerGeometry the pixel geometry of the inventory and its slot grid
 * @property backgroundGlyph whether the per-row background glyph of a custom inventory texture is
 *   rendered
 * @property rowFonts the fonts that render header text on a slot row, keyed by one-based row
 * @property cancelOnClick whether click events should be cancelled by default
 * @property cancelOnDrag whether drag events should be cancelled by default
 * @property cancelOnDrop whether drop events should be cancelled by default
 * @property cancelOnPickup whether pickup events should be cancelled by default
 * @property navigateBackOnOutsideClick whether an outside click navigates to the parent view
 * @property rows the number of rows displayed in the inventory
 * @see SurfViewSettings
 * @see SimpleViewSettingsBuilder
 */
data class SimpleViewSettings(
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
    override val rows: ViewRows = SurfViewSettingsDefaults.DEFAULT_VIEW_ROWS,
) : SurfViewSettings
