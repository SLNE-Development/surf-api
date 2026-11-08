package dev.slne.surf.api.paper.inventory.framework.view.util

import dev.slne.surf.api.core.inventory.framework.internal.ShiftGlyphs

/**
 * Produces the minimal string of shift-glyph characters that sum to the given [amount].
 *
 * This is a convenience wrapper around [GlyphShift.renderGlyphs].
 *
 * ```kotlin
 * // Shift right by 10 pixels: produces 2 glyphs (8 + 2)
 * val rightShift = shift(10)
 * // Shift left by 32 pixels: produces 1 glyph (-32)
 * val leftShift = shift(-32)
 * ```
 *
 * @param amount the total pixel shift; positive shifts right, negative shifts left
 * @return a string of resource-pack font glyph characters representing the shift, or `""` if zero
 * @see GlyphShift
 */
@Suppress("NOTHING_TO_INLINE")
inline fun shift(amount: Int) = GlyphShift(amount).renderGlyphs()

/**
 * Represents a horizontal pixel shift
 * composed of special font glyphs whose advances are powers of two.
 *
 * The [amount] may be negative (shift left) or positive (shift right).
 * Calling [renderGlyphs] produces the shortest possible string of
 * shift-glyph characters that sum to the requested [amount] by
 * performing a greedy binary decomposition.
 *
 * The glyph table and the decomposition algorithm are shared with the other platform
 * implementations and live in [ShiftGlyphs].
 *
 * @property amount the total pixel shift to represent
 */
@JvmInline
value class GlyphShift(val amount: Int) {

    /**
     * Renders the minimal sequence of shift glyphs whose advances sum
     * to [amount].
     *
     * Delegates to [ShiftGlyphs.renderShift].
     *
     * @return a [String] of shift-glyph characters, or an empty string
     *         if [amount] is zero
     * @throws IllegalStateException if a required glyph is missing from
     *         the shared glyph registry
     */
    fun renderGlyphs(): String = ShiftGlyphs.renderShift(amount)
}
