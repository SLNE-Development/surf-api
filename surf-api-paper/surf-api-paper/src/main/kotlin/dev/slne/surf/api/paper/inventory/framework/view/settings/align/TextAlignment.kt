package dev.slne.surf.api.paper.inventory.framework.view.settings.align

import dev.slne.surf.api.core.inventory.framework.internal.TextAlignmentMath

/**
 * Defines the horizontal alignment of title text within the inventory header container area.
 *
 * Each entry implements [calculateShift] to return the pixel offset that positions the text
 * at the correct horizontal position given the container geometry described by [TextAlignmentOptions].
 *
 * Alignment is driven by the **rendered** pixel width of the title rather than by its character
 * count, so uppercasing, per-character width overrides and the leading spacing glyph are all
 * accounted for. Use [calculateTextWidth] to measure a string that is rendered verbatim.
 *
 * @see TextAlignmentOptions
 * @see dev.slne.surf.api.paper.api.inventory.framework.view.settings.SurfViewSettings.headerTextAlignment
 */
enum class TextAlignment {
    /**
     * Aligns the title to the left edge of the container area (plus [TextAlignmentOptions.padding]).
     */
    LEFT {
        override fun calculateShift(textWidth: Int, options: TextAlignmentOptions): Int =
            TextAlignmentMath.leftAlignedShift(options.leftShift, options.padding)
    },

    /**
     * Aligns the title to the right edge of the container area.
     */
    RIGHT {
        override fun calculateShift(
            textWidth: Int,
            options: TextAlignmentOptions
        ): Int = TextAlignmentMath.rightAlignedShift(
            textWidth,
            options.leftShift,
            options.padding,
            options.containerWidth
        )
    },

    /**
     * Centers the title horizontally within the container area.
     */
    CENTER {
        override fun calculateShift(
            textWidth: Int,
            options: TextAlignmentOptions
        ): Int = TextAlignmentMath.centerAlignedShift(
            textWidth,
            options.leftShift,
            options.padding,
            options.containerWidth
        )
    };

    /**
     * Calculates the pixel shift (offset from the left edge) required to place a text run of
     * [textWidth] pixels at this alignment within the container described by [options].
     *
     * @param textWidth the rendered pixel width of the title, as measured by [calculateTextWidth]
     * @param options the container geometry options
     * @return the pixel shift value to apply before rendering the text
     */
    abstract fun calculateShift(textWidth: Int, options: TextAlignmentOptions): Int

    companion object {
        /**
         * Computes the total rendered pixel width of [text] given [options].
         *
         * Sums the per-glyph widths from [TextAlignmentOptions.charWidths] (falling back to
         * [TextAlignmentOptions.charSize]) and adds [TextAlignmentOptions.charSpacing] between
         * adjacent glyphs. Measured per Unicode code point. Returns `0` for an empty string.
         *
         * [text] must be the string as it is actually rendered — measuring a string that is
         * transformed before rendering (for example uppercased) yields the wrong width.
         *
         * @param text the string to measure
         * @param options the character size and spacing options
         * @return the total pixel width of the text
         */
        fun calculateTextWidth(text: String, options: TextAlignmentOptions): Int =
            TextAlignmentMath.textWidth(
                text,
                options.charSize,
                options.charSpacing,
                options.charWidths
            )
    }
}
