package dev.slne.surf.api.paper.inventory.framework.view.settings.align

import it.unimi.dsi.fastutil.ints.Int2IntMap
import it.unimi.dsi.fastutil.ints.Int2IntMaps

/**
 * Encapsulates the geometric parameters of the inventory title container area
 * used by [TextAlignment.calculateShift] and [TextAlignment.calculateTextWidth].
 *
 * @property leftShift the base pixel offset from the very left edge of the rendering canvas
 *   to the left edge of the title container area
 * @property padding horizontal padding (in pixels) applied on each side within the container
 * @property containerWidth the total usable pixel width of the title container area
 * @property charSize the width in pixels of a single character in the title font
 * @property charSpacing the inter-character spacing in pixels (negative = tighter, positive = looser)
 * @property charWidths per-code-point pixel width overrides, keyed by Unicode code point, for the
 *   glyphs the title font does not render at [charSize] pixels. Empty by default, which treats the
 *   font as fixed-width. Populate it for a proportional title font, or for characters the font does
 *   not cover and that therefore fall back to the client's default font at a different width.
 * @see TextAlignment
 */
data class TextAlignmentOptions(
    val leftShift: Int,
    val padding: Int,
    val containerWidth: Int,
    val charSize: Int,
    val charSpacing: Int,
    val charWidths: Int2IntMap = Int2IntMaps.EMPTY_MAP
)
