package dev.slne.surf.api.minestom.inventory.framework.view.container.component.components

import dev.slne.surf.api.core.inventory.framework.internal.ViewSlotGeometry
import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.core.util.freeze
import dev.slne.surf.api.minestom.inventory.framework.view.container.component.ViewContainerComponent
import dev.slne.surf.api.minestom.inventory.framework.view.settings.ViewHeaderGeometry
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap

/**
 * A [ViewContainerComponent] that renders a single inventory cell block overlay at the given
 * [column] and [row] position.
 *
 * Each instance renders one slot-sized glyph (plus 1 pixel spacing) that visually "blocks" a cell
 * of the inventory. The horizontal position is computed from [ViewHeaderGeometry]; the row cannot
 * be — a glyph's vertical position is baked into the `ascent` of its font provider in the resource
 * pack — so the glyph character itself is picked per [row] from [BlockRow].
 *
 * Blocking a cell is **purely cosmetic**: it draws a texture into the inventory title and touches
 * neither the slot's contents nor its click handling, so items can still be placed into and taken
 * out of a blocked slot exactly as before (subject to the view's usual
 * [cancelOnClick][dev.slne.surf.api.minestom.inventory.framework.view.settings.SurfViewSettings.cancelOnClick]
 * settings).
 *
 * Typically used via the DSL helpers
 * [blockCell][dev.slne.surf.api.minestom.inventory.framework.view.container.dsl.blockCell],
 * [blockSlot][dev.slne.surf.api.minestom.inventory.framework.view.container.dsl.blockSlot],
 * [blockRow][dev.slne.surf.api.minestom.inventory.framework.view.container.dsl.blockRow] or
 * [blockAllSlots][dev.slne.surf.api.minestom.inventory.framework.view.container.dsl.blockAllSlots].
 *
 * @property column the zero-based column index (0–8) of the cell to block
 * @property row the one-based row index (1–6) of the cell to block
 * @param geometry the header [ViewHeaderGeometry] describing the slot grid
 */
class ViewBlockCellComponent(
    val column: Int,
    val row: Int,
    geometry: ViewHeaderGeometry = ViewHeaderGeometry.DEFAULT,
) : ViewContainerComponent {

    /** @suppress binary compatibility, blocks the cell with the default geometry */
    @Deprecated(
        "Binary compatibility",
        ReplaceWith("ViewBlockCellComponent(column, row)"),
        DeprecationLevel.HIDDEN
    )
    constructor(column: Int, row: Int) : this(column, row, ViewHeaderGeometry.DEFAULT)

    init {
        require(column in 0 until ViewSlotGeometry.COLUMNS) {
            "Column must be in 0..${ViewSlotGeometry.COLUMNS - 1}, was $column"
        }
        require(row in 1..ViewSlotGeometry.MAX_ROWS) {
            "Row must be in 1..${ViewSlotGeometry.MAX_ROWS}, was $row"
        }
    }

    override val textureWidth = geometry.slotSize + 1 // 1 pixel for spacing
    override val positionalShift = geometry.slotGlyphShift(column)

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

    companion object {
        /**
         * Returns the component that blocks the cell at the zero-based [slot] index, counted
         * left-to-right and then top-to-bottom just like the inventory's own slot numbering.
         *
         * @param slot the zero-based slot index (0–53)
         * @param geometry the header [ViewHeaderGeometry] describing the slot grid
         */
        fun forSlot(
            slot: Int,
            geometry: ViewHeaderGeometry = ViewHeaderGeometry.DEFAULT,
        ): ViewBlockCellComponent = ViewBlockCellComponent(
            column = ViewSlotGeometry.columnOf(slot),
            row = ViewSlotGeometry.rowOf(slot),
            geometry = geometry
        )
    }


    /**
     * Maps a row number to its corresponding block-cell glyph character.
     *
     * One glyph per row: the textures are identical, but each one is declared with the `ascent`
     * that drops it onto its row, which is the only way to position a header texture vertically.
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
