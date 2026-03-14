package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.util

import dev.slne.surf.surfapi.core.api.util.freeze
import glm_.func.common.abs
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap

@Suppress("NOTHING_TO_INLINE")
inline fun shift(amount: Int) = GlyphShift(amount).renderGlyphs()

/**
 * Represents a horizontal pixel shift
 * composed of special font glyphs whose advances are powers of two.
 *
 * The [amount] may be negative (shift left) or positive (shift right).
 * Calling [renderGlyphs] produces the shortest possible string of
 * [ShiftGlyph] characters that sum to the requested [amount] by
 * performing a greedy binary decomposition.
 *
 * @property amount the total pixel shift to represent
 */
@JvmInline
value class GlyphShift(val amount: Int) {

    /**
     * Renders the minimal sequence of shift glyphs whose advances sum
     * to [amount].
     *
     * The algorithm works by repeatedly extracting the highest set bit
     * of the remaining absolute value (clamped to [ShiftGlyph.MAX_GLYPH_AMOUNT]),
     * looking up the corresponding glyph, and appending it. Because every
     * glyph amount is a power of two, this is equivalent to a greedy
     * binary decomposition and always yields the shortest result.
     *
     * @return a [String] of shift-glyph characters, or an empty string
     *         if [amount] is zero
     * @throws IllegalStateException if a required glyph is missing from
     *         the [ShiftGlyph] registry
     */
    fun renderGlyphs(): String {
        if (amount == 0) return ""

        val sign = if (amount < 0) -1 else 1

        return buildString {
            var remaining = amount.abs
            while (remaining > 0) {
                val largest = remaining.takeHighestOneBit()
                val glyphAmount = largest.coerceAtMost(ShiftGlyph.MAX_GLYPH_AMOUNT)
                val glyph = ShiftGlyph.fromAmount(glyphAmount * sign)
                    ?: error("Could not find glyph for amount ${glyphAmount * sign}")

                append(glyph.glyph)
                remaining -= glyphAmount
            }
        }
    }

    /**
     * A font glyph whose sole purpose is to shift the cursor by a
     * fixed number of pixels. Each glyph's [amount] is a power of two
     * (1, 2, 4, …, 512), available in both negative and positive variants.
     *
     * @property glyph  the Unicode character mapped to this shift in the resource pack
     * @property amount the pixel offset this glyph applies (negative = left, positive = right)
     */
    internal enum class ShiftGlyph(
        val glyph: Char,
        val amount: Int,
    ) {
        MINUS_ONE('', -1),
        MINUS_TWO('', -2),
        MINUS_FOUR('', -4),
        MINUS_EIGHT('', -8),
        MINUS_SIXTEEN('', -16),
        MINUS_THIRTY_TWO('', -32),
        MINUS_SIXTY_FOUR('', -64),
        MINUS_ONE_TWENTY_EIGHT('', -128),
        MINUS_TWO_FIFTY_SIX('', -256),
        MINUS_FIVE_TWELVE('', -512),
        PLUS_ONE('', 1),
        PLUS_TWO('', 2),
        PLUS_FOUR('', 4),
        PLUS_EIGHT('', 8),
        PLUS_SIXTEEN('', 16),
        PLUS_THIRTY_TWO('', 32),
        PLUS_SIXTY_FOUR('', 64),
        PLUS_ONE_TWENTY_EIGHT('', 128),
        PLUS_TWO_FIFTY_SIX('', 256),
        PLUS_FIVE_TWELVE('', 512);

        init {
            val abs = amount.abs
            require(abs > 0 && abs and (abs - 1) == 0) { "Glyph amount must be a power of two, was $amount" }
        }

        companion object {
            /** The largest absolute pixel shift a single glyph can represent. */
            const val MAX_GLYPH_AMOUNT = 512

            /** lookup table mapping from [amount] to [ShiftGlyph]. */
            private val index = entries.associateByTo(Int2ObjectOpenHashMap()) { it.amount }.freeze()

            /**
             * Returns the [ShiftGlyph] whose [ShiftGlyph.amount] equals [amount],
             * or `null` if no such glyph exists.
             *
             * @param amount the exact pixel shift to look up
             * @return the matching glyph, or `null`
             */
            fun fromAmount(amount: Int): ShiftGlyph? = index[amount]
        }
    }
}