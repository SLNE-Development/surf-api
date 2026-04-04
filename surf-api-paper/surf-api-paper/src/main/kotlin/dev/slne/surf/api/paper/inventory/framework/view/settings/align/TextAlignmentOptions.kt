package dev.slne.surf.api.paper.inventory.framework.view.settings.align

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
 * @see TextAlignment
 */
data class TextAlignmentOptions(
    val leftShift: Int,
    val padding: Int,
    val containerWidth: Int,
    val charSize: Int,
    val charSpacing: Int
)
