package dev.slne.surf.api.minestom.inventory.framework.view.pagination

import dev.slne.surf.api.core.inventory.framework.internal.PaginationButtonGlyphs
import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.minestom.inventory.framework.view.container.component.ViewContainerComponent
import dev.slne.surf.api.minestom.inventory.framework.view.settings.ViewHeaderGeometry
import me.devnatan.inventoryframework.component.Pagination

/**
 * A [ViewContainerComponent] that renders the pagination button overlay glyph in the inventory header.
 *
 * There are four visual states depending on whether the left and right navigation buttons are
 * available ([Pagination.canBack] / [Pagination.canAdvance]):
 * - [Disabled] — neither button is available
 * - [DisabledLeft] — only the right (next) button is available
 * - [DisabledRight] — only the left (previous) button is available
 * - [Enabled] — both buttons are available
 *
 * Each subclass delegates its per-row glyph lookup to the shared [PaginationButtonGlyphs] table.
 * The correct glyph character is selected based on the [row] (1-based) in which the buttons appear.
 * The overlay is positioned on the column of the left navigation button (see [PaginationButton]),
 * plus the [GLYPH_INSET] the texture keeps free on its left, and spans [TEXTURE_WIDTH] pixels.
 *
 * Use [getByPaginationState] to obtain the correct instance for a given [Pagination] state.
 *
 * @param row the 1-based inventory row where the pagination buttons are located
 * @param geometry the header [ViewHeaderGeometry] describing the slot grid
 */
internal sealed class PaginationButtonGlyphComponent(
    private val row: Int,
    geometry: ViewHeaderGeometry = ViewHeaderGeometry.DEFAULT,
) : ViewContainerComponent {
    override val positionalShift = geometry.columnShift(PaginationButton.LEFT.column) + GLYPH_INSET
    override val textureWidth = TEXTURE_WIDTH

    /**
     * Returns the glyph character for this button state at the given [rows] (1-based row index).
     *
     * @param rows the 1-based row number of the button row
     * @return the corresponding Unicode glyph character
     * @throws IllegalStateException if [rows] is outside the valid range 1..6
     */
    abstract fun glyph(rows: Int): Char

    override fun SurfComponentBuilder.renderComponent() {
        text(glyph(row))
        color(Colors.WHITE)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PaginationButtonGlyphComponent) return false

        if (row != other.row) return false

        return true
    }

    override fun hashCode(): Int {
        return row
    }

    /** Both navigation buttons are disabled (no previous and no next page). */
    class Disabled(
        row: Int,
        geometry: ViewHeaderGeometry = ViewHeaderGeometry.DEFAULT,
    ) : PaginationButtonGlyphComponent(row, geometry) {
        override fun glyph(rows: Int): Char = PaginationButtonGlyphs.DISABLED.glyph(rows)
    }

    /** The right (next) button is disabled; only the left (previous) button is active. */
    class DisabledRight(
        row: Int,
        geometry: ViewHeaderGeometry = ViewHeaderGeometry.DEFAULT,
    ) : PaginationButtonGlyphComponent(row, geometry) {
        override fun glyph(rows: Int): Char = PaginationButtonGlyphs.DISABLED_RIGHT.glyph(rows)
    }

    /** The left (previous) button is disabled; only the right (next) button is active. */
    class DisabledLeft(
        row: Int,
        geometry: ViewHeaderGeometry = ViewHeaderGeometry.DEFAULT,
    ) : PaginationButtonGlyphComponent(row, geometry) {
        override fun glyph(rows: Int): Char = PaginationButtonGlyphs.DISABLED_LEFT.glyph(rows)
    }

    /** Both navigation buttons are active (there are previous and next pages). */
    class Enabled(
        row: Int,
        geometry: ViewHeaderGeometry = ViewHeaderGeometry.DEFAULT,
    ) : PaginationButtonGlyphComponent(row, geometry) {
        override fun glyph(rows: Int): Char = PaginationButtonGlyphs.ENABLED.glyph(rows)
    }

    companion object {
        /**
         * Pixels the button overlay glyph advances the render cursor by, measured off the glyph
         * sheet of the pack: `menu/pagination/buttons.png` is 88x48, i.e. four 88x12 bands, and
         * rendering a band one-to-one (`height: 12`) advances its 88 pixels plus the one the font
         * renderer adds.
         *
         * This has to match what the glyph really advances - the container resets the cursor by
         * `-(textureWidth + positionalShift)` after every component, so a wrong value shifts every
         * component rendered afterwards.
         */
        const val TEXTURE_WIDTH = 89

        /**
         * Pixels the overlay is moved relative to the left edge of the left button slot, tuned
         * against the texture of the pack in-game.
         *
         * Note that the texture cannot line up with both buttons at once: the centres of its two
         * arrows are 73 pixels apart (ink columns 0-14 and 73-87 of the 88 pixel bar), while the
         * slot grid puts the two button slots 72 pixels apart. Whatever this offset is, one arrow
         * ends up a pixel off its slot centre - move the right arrow one pixel left in
         * `menu/pagination/buttons.png` to fix that at the source.
         */
        const val GLYPH_INSET = 0

        /**
         * Pixel offset of the free area between the two arrows, measured from the left edge of the
         * bar: the arrows of `menu/pagination/buttons.png` occupy its first and last 15 pixels.
         */
        const val COUNTER_AREA_OFFSET = 15

        /** Width in pixels of the free area between the two arrows. */
        const val COUNTER_AREA_WIDTH = 58

        /**
         * Returns the pixel offset of the free area between the two arrows, relative to the title
         * origin - where the page counter of a paginated view belongs.
         *
         * @param geometry the header geometry describing the slot grid the bar is placed on
         */
        fun counterAreaShift(geometry: ViewHeaderGeometry): Int =
            geometry.columnShift(PaginationButton.LEFT.column) + GLYPH_INSET + COUNTER_AREA_OFFSET


        /**
         * Returns the appropriate [PaginationButtonGlyphComponent] for the given [pagination] state.
         *
         * Selects the subclass based on whether [Pagination.canBack] and [Pagination.canAdvance]
         * are true or false.
         *
         * @param row the 1-based row index where the buttons are located
         * @param pagination the current [Pagination] state
         * @param geometry the header [ViewHeaderGeometry] describing the slot grid
         * @return the matching [PaginationButtonGlyphComponent]
         */
        fun getByPaginationState(
            row: Int,
            pagination: Pagination,
            geometry: ViewHeaderGeometry = ViewHeaderGeometry.DEFAULT,
        ): PaginationButtonGlyphComponent =
            when {
                !pagination.canBack() && !pagination.canAdvance() -> Disabled(row, geometry)
                !pagination.canBack() && pagination.canAdvance() -> DisabledLeft(row, geometry)
                pagination.canBack() && !pagination.canAdvance() -> DisabledRight(row, geometry)
                pagination.canBack() && pagination.canAdvance() -> Enabled(row, geometry)
                else -> throw MatchException(
                    "Invalid pagination state: canBack=${pagination.canBack()}, canAdvance=${pagination.canAdvance()}",
                    null
                )
            }
    }
}