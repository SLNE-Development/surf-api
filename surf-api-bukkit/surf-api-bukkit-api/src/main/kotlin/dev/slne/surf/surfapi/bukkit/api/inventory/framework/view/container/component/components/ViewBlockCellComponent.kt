package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.component.components

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.component.ViewContainerComponent
import dev.slne.surf.surfapi.core.api.messages.Colors
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import dev.slne.surf.surfapi.core.api.util.freeze
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap

/**
 * A [ViewContainerComponent] that renders a single inventory cell block overlay at the given
 * [column] and [row] position.
 *
 * Each instance renders a 18×18 pixel glyph (plus 1 pixel spacing = 19 pixels wide) that
 * visually "blocks" a specific cell in the inventory header texture. The glyph character is
 * looked up from [BlockRow] based on the [row] parameter.
 *
 * Typically used via the DSL helper [dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.dsl.blockCell]
 * or [dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.dsl.blockRow].
 *
 * @property column the zero-based column index (0–8) of the cell to block
 * @property row the one-based row index (1–6) of the cell to block
 */
class ViewBlockCellComponent(
    val column: Int,
    val row: Int
) : ViewContainerComponent {
    override val textureWidth = 18 + 1 // 1 pixel for spacing
    override val positionalShift = 1 + column * (textureWidth - 1)

    override fun SurfComponentBuilder.renderComponent() {
        val blockRow = BlockRow.fromRow(row) ?: return
        text(blockRow.glyph)
        color(Colors.WHITE)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ViewBlockCellComponent) return false

        if (column != other.column) return false
        if (row != other.row) return false

        return true
    }

    override fun hashCode(): Int {
        var result = column
        result = 31 * result + row
        return result
    }


    /**
     * Maps a row number to its corresponding block-cell glyph character.
     *
     * @property glyph the Unicode character in the resource-pack font that draws the cell overlay
     * @property row the one-based inventory row this glyph corresponds to (1–6)
     */
    internal enum class BlockRow(
        val glyph: Char,
        val row: Int
    ) {
        /** Block-cell glyph for row 1. */
        ONE('ꐱ', 1),

        /** Block-cell glyph for row 2. */
        TWO('ꐲ', 2),

        /** Block-cell glyph for row 3. */
        THREE('ꐳ', 3),

        /** Block-cell glyph for row 4. */
        FOUR('ꐴ', 4),

        /** Block-cell glyph for row 5. */
        FIVE('ꐵ', 5),

        /** Block-cell glyph for row 6. */
        SIX('ꐶ', 6);

        companion object {
            private val index = entries.associateByTo(Int2ObjectOpenHashMap(6)) { it.row }.freeze()

            /**
             * Returns the [BlockRow] for the given one-based [row] number, or `null` if not found.
             *
             * @param row the one-based row number (1–6)
             * @return the matching [BlockRow], or `null`
             */
            fun fromRow(row: Int): BlockRow? = index[row]
        }
    }
}