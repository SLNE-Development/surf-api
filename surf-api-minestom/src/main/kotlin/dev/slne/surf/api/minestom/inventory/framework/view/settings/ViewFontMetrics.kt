package dev.slne.surf.api.minestom.inventory.framework.view.settings

import dev.slne.surf.api.core.inventory.framework.internal.DefaultFontWidths
import dev.slne.surf.api.core.util.int2IntMapOf
import it.unimi.dsi.fastutil.ints.Int2IntMap
import it.unimi.dsi.fastutil.ints.Int2IntMaps

/**
 * Metrics of the font a piece of header text is rendered in: how wide its glyphs are, how much
 * spacing goes between them, and whether it only draws capitals.
 *
 * Header text is positioned by shifting the render cursor, so its width has to be known before it
 * is drawn. That width can only be derived from the font: [VANILLA] describes the proportional
 * client font (widths from [DefaultFontWidths]), [SURF_MENU] the fixed-width resource-pack menu
 * font, and a custom instance any other font in the pack.
 *
 * ```kotlin
 * settings {
 *     // A row font drawn 10px per glyph with no extra spacing, capitals and lowercase:
 *     rowFontMetrics(ViewFontMetrics(charSize = 10))
 * }
 * ```
 *
 * @property charSize pixel advance of a glyph that has no entry in [charWidths]; for a bitmap font
 *   this is its cell width including the one pixel the font renderer adds after every glyph
 * @property charSpacing pixels of spacing inserted in front of every glyph — negative tightens the
 *   run, `0` renders the text as-is. The spacing is emitted as shift glyphs **inside** the text, so
 *   the font must provide them
 * @property uppercase whether the text is uppercased before rendering, for a font that only draws
 *   capitals
 * @property spaceAsShift whether a space is rendered as a shift glyph instead of as a space. A font
 *   built from a glyph sheet draws no space of its own, so the client would fall back to the
 *   missing-glyph box, which lands on the title line instead of in the row - rendering the space as
 *   a shift of [advance] pixels avoids depending on the font providing one
 * @property charWidths per-code-point advance overrides for the glyphs the font does not render at
 *   [charSize] pixels
 * @see SurfViewSettings.headerFontMetrics
 * @see SurfViewSettings.rowFontMetrics
 */
data class ViewFontMetrics(
    val charSize: Int = DefaultFontWidths.DEFAULT_ADVANCE,
    val charSpacing: Int = 0,
    val uppercase: Boolean = false,
    val spaceAsShift: Boolean = false,
    val charWidths: Int2IntMap = Int2IntMaps.EMPTY_MAP,
) {
    /**
     * Pixels a space advances when it is rendered as a shift glyph, or `null` when the font draws
     * the space itself.
     */
    val spaceShift: Int? get() = if (spaceAsShift) advance(' '.code) else null

    /**
     * Returns the pixel advance of [codePoint] in this font.
     *
     * @param codePoint the Unicode code point to measure
     * @param bold whether the glyph is rendered bold, which widens it by one pixel
     */
    fun advance(codePoint: Int, bold: Boolean = false): Int {
        val advance =
            if (charWidths.containsKey(codePoint)) charWidths.get(codePoint) else charSize

        return advance + if (bold) DefaultFontWidths.BOLD_EXTRA_ADVANCE else 0
    }

    companion object {
        /**
         * Metrics of the proportional client font (`minecraft:default`): per-glyph widths from
         * [DefaultFontWidths], no extra spacing, text rendered as written. The client font brings
         * its own space provider, so spaces are emitted as spaces.
         */
        val VANILLA = ViewFontMetrics(
            charSize = DefaultFontWidths.DEFAULT_ADVANCE,
            charWidths = DefaultFontWidths.advances
        )

        /**
         * Metrics of a row font that copies the client font glyph sheets onto a slot row: the same
         * widths as [VANILLA], but the space is rendered as a shift glyph, because such a copy
         * carries only the glyph sheets and no space provider.
         */
        val VANILLA_ROW = VANILLA.copy(spaceAsShift = true)

        /**
         * Metrics of the fixed-width resource-pack menu font, including the per-row fonts of
         * [SurfViewSettingsDefaults.rowFont]: 9-pixel cells tightened by one pixel — 8 pixels per
         * glyph — and uppercased, because the font only draws capitals.
         *
         * The space is the one exception: the font draws no space glyph, so it is rendered as a
         * shift of [MENU_SPACE_SIZE] pixels.
         */
        val SURF_MENU = ViewFontMetrics(
            charSize = MENU_CHAR_SIZE,
            charSpacing = MENU_CHAR_SPACING,
            uppercase = true,
            spaceAsShift = true,
            charWidths = int2IntMapOf(' '.code to MENU_SPACE_SIZE)
        )

        /** Pixel advance of one glyph cell in the resource-pack menu font. */
        const val MENU_CHAR_SIZE = 9

        /** Spacing that tightens the menu font to 8 pixels per glyph. */
        const val MENU_CHAR_SPACING = -1

        /**
         * Advance of a space in the menu font, taken from the space provider of the pack rather than
         * from the glyph texture.
         */
        const val MENU_SPACE_SIZE = 4
    }
}
