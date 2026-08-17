package dev.slne.surf.api.core.inventory.framework.internal

import dev.slne.surf.api.shared.api.util.InternalSurfApi

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
     * Computes the total rendered pixel width of [text].
     *
     * Uses the formula: `text.length * charSize + (text.length - 1) * charSpacing`.
     * Returns `0` for an empty string.
     *
     * @param text the string to measure
     * @param charSize the width in pixels of a single character
     * @param charSpacing the inter-character spacing in pixels
     * @return the total pixel width of the text
     */
    fun textWidth(text: String, charSize: Int, charSpacing: Int): Int {
        if (text.isEmpty()) return 0
        val n = text.length

        return (n * charSize) + ((n - 1) * charSpacing)
    }

    /**
     * Pixel shift that places text at the left edge of the container area.
     *
     * @param leftShift the base pixel offset of the container area
     * @param padding horizontal padding applied on each side within the container
     */
    fun leftAlignedShift(leftShift: Int, padding: Int): Int = leftShift + padding

    /**
     * Pixel shift that places [text] at the right edge of the container area.
     *
     * @param text the string whose width is taken into account
     * @param leftShift the base pixel offset of the container area
     * @param padding horizontal padding applied on each side within the container
     * @param containerWidth the total usable pixel width of the container area
     * @param charSize the width in pixels of a single character
     * @param charSpacing the inter-character spacing in pixels
     */
    fun rightAlignedShift(
        text: String,
        leftShift: Int,
        padding: Int,
        containerWidth: Int,
        charSize: Int,
        charSpacing: Int,
    ): Int {
        val usableWidth = containerWidth - (padding * 2)
        val freeSpace = usableWidth - textWidth(text, charSize, charSpacing)
        return leftShift + freeSpace + 1 + padding
    }

    /**
     * Pixel shift that centers [text] horizontally within the container area.
     *
     * @param text the string whose width is taken into account
     * @param leftShift the base pixel offset of the container area
     * @param padding horizontal padding applied on each side within the container
     * @param containerWidth the total usable pixel width of the container area
     * @param charSize the width in pixels of a single character
     * @param charSpacing the inter-character spacing in pixels
     */
    fun centerAlignedShift(
        text: String,
        leftShift: Int,
        padding: Int,
        containerWidth: Int,
        charSize: Int,
        charSpacing: Int,
    ): Int {
        val usableWidth = containerWidth - (padding * 2)
        val freeSpace = usableWidth - textWidth(text, charSize, charSpacing)
        return leftShift + (freeSpace / 2) + 1 + padding
    }
}
