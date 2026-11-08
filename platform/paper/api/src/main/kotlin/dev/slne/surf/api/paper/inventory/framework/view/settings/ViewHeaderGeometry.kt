package dev.slne.surf.api.paper.inventory.framework.view.settings

import dev.slne.surf.api.core.inventory.framework.internal.DefaultFontWidths
import dev.slne.surf.api.core.inventory.framework.internal.ViewSlotGeometry
import dev.slne.surf.api.paper.inventory.framework.view.settings.align.TextAlignment
import dev.slne.surf.api.paper.inventory.framework.view.settings.align.TextAlignmentOptions
import it.unimi.dsi.fastutil.ints.Int2IntMap

/**
 * Pixel geometry of the inventory header: how wide the inventory is and where its slot grid starts.
 *
 * Everything the header draws is a glyph inside the inventory title, so all of these values are
 * horizontal offsets measured from the **title origin** — the pixel the client starts drawing the
 * inventory title at. Negative values are left of it.
 *
 * The defaults describe the vanilla chest inventory, whose screen is 176 pixels wide with the title
 * drawn 8 pixels in from its left edge (hence [titleLeftShift] `= -8`) and whose slot cells are
 * 18 pixels apart starting one pixel left of the title origin (hence [slotOriginX] `= -1`). With
 * those numbers the centre of the inventory and the centre of the slot grid coincide, so a centred
 * title sits exactly above a centred row text. Only override them for a resource pack that replaces
 * the inventory background with a texture of a different size.
 *
 * ```kotlin
 * settings {
 *     headerGeometry { copy(titleWidth = 223, titleLeftShift = -48) }
 * }
 * ```
 *
 * @property titleLeftShift pixel offset of the left edge of the inventory, and therefore of the
 *   area the title is aligned in
 * @property titlePadding horizontal padding kept free on each side of the title area
 * @property titleWidth width of the inventory in pixels, i.e. the width the title is aligned in
 * @property slotOriginX pixel offset of the left edge of slot column `0`
 * @property slotSize horizontal pitch of the slot grid: one slot plus its one-pixel border
 * @property slotGlyphInset transparent padding on the left of the slot overlay texture, so a
 *   texture that does not start at its very first pixel still lands on the slot border
 * @property rowTextPadding horizontal padding kept free on each side when text is aligned inside a
 *   span of slot columns
 * @see SurfViewSettings.headerGeometry
 */
data class ViewHeaderGeometry(
    val titleLeftShift: Int = DEFAULT_TITLE_LEFT_SHIFT,
    val titlePadding: Int = DEFAULT_TITLE_PADDING,
    val titleWidth: Int = DEFAULT_TITLE_WIDTH,
    val slotOriginX: Int = DEFAULT_SLOT_ORIGIN_X,
    val slotSize: Int = ViewSlotGeometry.SLOT_SIZE,
    val slotGlyphInset: Int = DEFAULT_SLOT_GLYPH_INSET,
    val rowTextPadding: Int = DEFAULT_ROW_TEXT_PADDING,
) {
    /** Width in pixels of the full nine-column slot grid. */
    val slotAreaWidth: Int get() = ViewSlotGeometry.spanWidth(ViewSlotGeometry.COLUMNS, slotSize)

    /**
     * Returns the pixel offset of the left edge of [column].
     *
     * @param column the zero-based column index (`0`–`8`)
     */
    fun columnShift(column: Int): Int = ViewSlotGeometry.columnShift(column, slotOriginX, slotSize)

    /**
     * Returns the pixel offset a slot overlay texture is rendered at in [column]: the left edge of
     * the column, moved right by the [slotGlyphInset] the texture keeps free on its left.
     *
     * @param column the zero-based column index (`0`-`8`)
     */
    fun slotGlyphShift(column: Int): Int = columnShift(column) + slotGlyphInset

    /**
     * Returns the [TextAlignmentOptions] that align text across the full width of the inventory —
     * the geometry the inventory title itself uses.
     *
     * The returned options measure text in the vanilla font. Pass [charWidths] for glyphs
     * [DefaultFontWidths] does not cover (custom resource-pack glyphs, CJK from the Unifont
     * fallback), or a [charSize]/[charSpacing] pair describing a fixed-width resource-pack font.
     *
     * @param charWidths per-code-point advance overrides
     * @param charSize advance of a glyph without an override
     * @param charSpacing extra spacing inserted between adjacent glyphs
     */
    fun titleOptions(
        charWidths: Int2IntMap = DefaultFontWidths.advances,
        charSize: Int = DefaultFontWidths.DEFAULT_ADVANCE,
        charSpacing: Int = 0,
    ): TextAlignmentOptions = TextAlignmentOptions(
        leftShift = titleLeftShift,
        padding = titlePadding,
        containerWidth = titleWidth,
        charSize = charSize,
        charSpacing = charSpacing,
        charWidths = charWidths
    )

    /**
     * Returns the [TextAlignmentOptions] that align text inside the span of slot [columns], so a
     * [TextAlignment] positions it relative to those slots rather than to the whole inventory.
     *
     * @param columns the zero-based, inclusive range of slot columns to align inside
     * @param charWidths per-code-point advance overrides
     * @param charSize advance of a glyph without an override
     * @param charSpacing extra spacing inserted between adjacent glyphs
     */
    fun columnSpanOptions(
        columns: IntRange,
        charWidths: Int2IntMap = DefaultFontWidths.advances,
        charSize: Int = DefaultFontWidths.DEFAULT_ADVANCE,
        charSpacing: Int = 0,
    ): TextAlignmentOptions {
        require(!columns.isEmpty()) { "A column span must cover at least one column" }

        return TextAlignmentOptions(
            leftShift = columnShift(columns.first),
            padding = rowTextPadding,
            containerWidth = ViewSlotGeometry.spanWidth(columns.count(), slotSize),
            charSize = charSize,
            charSpacing = charSpacing,
            charWidths = charWidths
        )
    }

    companion object {
        /** Default [titleLeftShift]: the vanilla inventory starts 8 pixels left of the title origin. */
        const val DEFAULT_TITLE_LEFT_SHIFT = -8

        /** Default [titlePadding]: the title may use the full width of the inventory. */
        const val DEFAULT_TITLE_PADDING = 0

        /** Default [titleWidth]: the width of the vanilla chest inventory screen. */
        const val DEFAULT_TITLE_WIDTH = 176

        /** Default [slotOriginX]: slot column `0` starts one pixel left of the title origin. */
        const val DEFAULT_SLOT_ORIGIN_X = -1

        /**
         * Default [slotGlyphInset]: none. The slot overlay glyphs of the pack are 18x18 textures that
         * start at their first pixel, so they land on the cell border the grid is measured from.
         * Set it to the number of transparent columns your texture carries on its left instead of
         * re-cutting the texture.
         */
        const val DEFAULT_SLOT_GLYPH_INSET = 0

        /** Default [rowTextPadding]: one pixel of breathing room inside a column span. */
        const val DEFAULT_ROW_TEXT_PADDING = 1

        /** The geometry of the vanilla chest inventory. */
        val DEFAULT = ViewHeaderGeometry()
    }
}
