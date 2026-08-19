package dev.slne.surf.api.core.inventory.framework.internal

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import it.unimi.dsi.fastutil.ints.Int2IntMap
import it.unimi.dsi.fastutil.ints.Int2IntMaps

/**
 * Shared pixel math behind the per-platform `TextAlignment` enums.
 *
 * All functions operate on plain integers describing the geometry of the inventory title
 * container area (see the platform `TextAlignmentOptions`), so the alignment formulas live in
 * exactly one place.
 *
 * This is internal infrastructure — use the platform `TextAlignment` enum instead.
 */
@InternalSurfApi
object TextAlignmentMath {

    /**
     * Returns the rendered pixel width of the glyph for [codePoint].
     *
     * Looks [codePoint] up in [charWidths] and falls back to [charSize] when the font renders
     * it at the default width.
     *
     * @param codePoint the Unicode code point to measure
     * @param charSize the width in pixels of a single character in the title font
     * @param charWidths per-code-point width overrides for glyphs that are not [charSize] wide
     */
    fun charWidth(
        codePoint: Int,
        charSize: Int,
        charWidths: Int2IntMap = Int2IntMaps.EMPTY_MAP,
    ): Int = if (charWidths.containsKey(codePoint)) charWidths.get(codePoint) else charSize

    /**
     * Computes the total rendered pixel width of [text].
     *
     * Sums the width of every glyph (see [charWidth]) and adds [charSpacing] between each pair of
     * adjacent glyphs. Returns `0` for an empty string.
     *
     * Measurement is done per Unicode **code point**, not per `Char`, so surrogate pairs count as
     * the single glyph they render as.
     *
     * @param text the string to measure, exactly as it is rendered
     * @param charSize the width in pixels of a single character
     * @param charSpacing the inter-character spacing in pixels
     * @param charWidths per-code-point width overrides for glyphs that are not [charSize] wide
     * @return the total pixel width of the text
     */
    fun textWidth(
        text: String,
        charSize: Int,
        charSpacing: Int,
        charWidths: Int2IntMap = Int2IntMaps.EMPTY_MAP,
    ): Int {
        val glyphs = text.codePointCount(0, text.length)
        if (glyphs == 0) return 0

        var width = (glyphs - 1) * charSpacing
        var index = 0
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            width += charWidth(codePoint, charSize, charWidths)
            index += Character.charCount(codePoint)
        }

        return width
    }

    /**
     * Pixel shift that places text at the left edge of the container area.
     *
     * @param leftShift the base pixel offset of the container area
     * @param padding horizontal padding applied on each side within the container
     */
    fun leftAlignedShift(leftShift: Int, padding: Int): Int = leftShift + padding

    /**
     * Pixel shift that places a run of [textWidth] pixels at the right edge of the container area.
     *
     * @param textWidth the rendered pixel width of the text, as returned by [textWidth]
     * @param leftShift the base pixel offset of the container area
     * @param padding horizontal padding applied on each side within the container
     * @param containerWidth the total usable pixel width of the container area
     */
    fun rightAlignedShift(
        textWidth: Int,
        leftShift: Int,
        padding: Int,
        containerWidth: Int,
    ): Int {
        val usableWidth = containerWidth - (padding * 2)
        val freeSpace = usableWidth - textWidth

        return leftShift + freeSpace + 1 + padding
    }

    /**
     * Pixel shift that centers a run of [textWidth] pixels horizontally within the container area.
     *
     * The free space is halved with [Math.floorDiv] so that a title wider than the container (which
     * yields a negative free space) overflows evenly instead of being biased to one side by
     * truncation towards zero.
     *
     * @param textWidth the rendered pixel width of the text, as returned by [textWidth]
     * @param leftShift the base pixel offset of the container area
     * @param padding horizontal padding applied on each side within the container
     * @param containerWidth the total usable pixel width of the container area
     */
    fun centerAlignedShift(
        textWidth: Int,
        leftShift: Int,
        padding: Int,
        containerWidth: Int,
    ): Int {
        val usableWidth = containerWidth - (padding * 2)
        val freeSpace = usableWidth - textWidth

        return leftShift + Math.floorDiv(freeSpace, 2) + 1 + padding
    }
}
