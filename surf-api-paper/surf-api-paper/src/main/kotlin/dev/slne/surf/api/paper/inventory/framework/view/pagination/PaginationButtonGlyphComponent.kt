package dev.slne.surf.api.paper.inventory.framework.view.pagination

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.paper.inventory.framework.view.container.component.ViewContainerComponent
import dev.slne.surf.api.paper.inventory.framework.view.pagination.PaginationButtonGlyphComponent.Companion.getByPaginationState
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
 * Each subclass contains a per-row glyph lookup. The correct glyph character is selected based
 * on the [row] (1-based) in which the buttons appear. The component has a fixed positional shift
 * of 38 pixels and a texture width of 88 pixels.
 *
 * Use [getByPaginationState] to obtain the correct instance for a given [Pagination] state.
 *
 * @param row the 1-based inventory row where the pagination buttons are located
 */
internal sealed class PaginationButtonGlyphComponent(private val row: Int) :
    ViewContainerComponent {
    override val positionalShift = 39
    override val textureWidth = 88

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
    class Disabled(row: Int) : PaginationButtonGlyphComponent(row) {
        override fun glyph(rows: Int): Char = when (rows) {
            1 -> 'ꐕ'
            2 -> 'ꐙ'
            3 -> 'ꐝ'
            4 -> 'ꐡ'
            5 -> 'ꐥ'
            6 -> 'ꐩ'
            else -> error("Invalid row: $rows")
        }
    }

    /** The right (next) button is disabled; only the left (previous) button is active. */
    class DisabledRight(row: Int) : PaginationButtonGlyphComponent(row) {
        override fun glyph(rows: Int): Char = when (rows) {
            1 -> 'ꐖ'
            2 -> 'ꐚ'
            3 -> 'ꐞ'
            4 -> 'ꐢ'
            5 -> 'ꐦ'
            6 -> 'ꐪ'
            else -> error("Invalid row: $rows")
        }
    }

    /** The left (previous) button is disabled; only the right (next) button is active. */
    class DisabledLeft(row: Int) : PaginationButtonGlyphComponent(row) {
        override fun glyph(rows: Int): Char = when (rows) {
            1 -> 'ꐗ'
            2 -> 'ꐛ'
            3 -> 'ꐟ'
            4 -> 'ꐣ'
            5 -> 'ꐧ'
            6 -> 'ꐫ'
            else -> error("Invalid row: $rows")
        }
    }

    /** Both navigation buttons are active (there are previous and next pages). */
    class Enabled(row: Int) : PaginationButtonGlyphComponent(row) {
        override fun glyph(rows: Int): Char = when (rows) {
            1 -> 'ꐘ'
            2 -> 'ꐜ'
            3 -> 'ꐠ'
            4 -> 'ꐤ'
            5 -> 'ꐨ'
            6 -> 'ꐬ'
            else -> error("Invalid row: $rows")
        }
    }

    companion object {
        /**
         * Returns the appropriate [PaginationButtonGlyphComponent] for the given [pagination] state.
         *
         * Selects the subclass based on whether [Pagination.canBack] and [Pagination.canAdvance]
         * are true or false.
         *
         * @param row the 1-based row index where the buttons are located
         * @param pagination the current [Pagination] state
         * @return the matching [PaginationButtonGlyphComponent]
         */
        fun getByPaginationState(row: Int, pagination: Pagination): PaginationButtonGlyphComponent =
            when {
                !pagination.canBack() && !pagination.canAdvance() -> Disabled(row)
                !pagination.canBack() && pagination.canAdvance() -> DisabledLeft(row)
                pagination.canBack() && !pagination.canAdvance() -> DisabledRight(row)
                pagination.canBack() && pagination.canAdvance() -> Enabled(row)
                else -> throw MatchException(
                    "Invalid pagination state: canBack=${pagination.canBack()}, canAdvance=${pagination.canAdvance()}",
                    null
                )
            }
    }
}