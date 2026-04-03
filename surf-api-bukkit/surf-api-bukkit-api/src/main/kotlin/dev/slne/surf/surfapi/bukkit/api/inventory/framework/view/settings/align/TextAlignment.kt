package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.align

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.align.TextAlignment.Companion.calculateTextWidth


/**
 * Defines the horizontal alignment of title text within the inventory header container area.
 *
 * Each entry implements [calculateShift] to return the pixel offset that positions the text
 * at the correct horizontal position given the container geometry described by [TextAlignmentOptions].
 *
 * The companion object provides [calculateTextWidth] which computes the total pixel width of a
 * text string based on the character size and spacing configured in [TextAlignmentOptions].
 *
 * @see TextAlignmentOptions
 * @see dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.SurfViewSettings.headerTextAlignment
 */
enum class TextAlignment {
    /**
     * Aligns the title to the left edge of the container area (plus [TextAlignmentOptions.padding]).
     */
    LEFT {
        override fun calculateShift(text: String, options: TextAlignmentOptions): Int =
            options.leftShift + options.padding
    },

    /**
     * Aligns the title to the right edge of the container area.
     */
    RIGHT {
        override fun calculateShift(
            text: String,
            options: TextAlignmentOptions
        ): Int {
            val usableWidth = options.containerWidth - (options.padding * 2)
            val freeSpace = usableWidth - calculateTextWidth(text, options)
            return options.leftShift + freeSpace + 1 + options.padding
        }
    },

    /**
     * Centers the title horizontally within the container area.
     */
    CENTER {
        override fun calculateShift(
            text: String,
            options: TextAlignmentOptions
        ): Int {
            val usableWidth = options.containerWidth - (options.padding * 2)
            val freeSpace = usableWidth - calculateTextWidth(text, options)
            return options.leftShift + (freeSpace / 2) + 1 + options.padding
        }
    };

    /**
     * Calculates the pixel shift (offset from the left edge) required to place [text] at this alignment
     * within the container described by [options].
     *
     * @param text the title string whose width is taken into account
     * @param options the container geometry options
     * @return the pixel shift value to apply before rendering the text
     */
    abstract fun calculateShift(text: String, options: TextAlignmentOptions): Int

    companion object {
        /**
         * Computes the total rendered pixel width of [text] given [options].
         *
         * Uses the formula: `text.length * charSize + (text.length - 1) * charSpacing`.
         * Returns `0` for an empty string.
         *
         * @param text the string to measure
         * @param options the character size and spacing options
         * @return the total pixel width of the text
         */
        fun calculateTextWidth(text: String, options: TextAlignmentOptions): Int {
            if (text.isEmpty()) return 0
            val n = text.length

            return (n * options.charSize) + ((n - 1) * options.charSpacing)
        }
    }
}