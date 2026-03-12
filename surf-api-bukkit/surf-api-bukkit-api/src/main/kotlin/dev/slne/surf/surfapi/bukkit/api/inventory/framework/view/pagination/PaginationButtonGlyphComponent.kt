package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.pagination

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.component.ViewContainerComponent
import dev.slne.surf.surfapi.core.api.messages.Colors
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import me.devnatan.inventoryframework.component.Pagination

internal sealed class PaginationButtonGlyphComponent(private val row: Int) : ViewContainerComponent {
    override val positionalShift = 38
    override val textureWidth = 88

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