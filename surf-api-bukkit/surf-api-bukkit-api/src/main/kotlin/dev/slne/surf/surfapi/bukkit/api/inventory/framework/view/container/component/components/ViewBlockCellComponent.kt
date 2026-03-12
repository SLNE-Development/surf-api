package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.component.components

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.component.ViewContainerComponent
import dev.slne.surf.surfapi.core.api.messages.Colors
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import dev.slne.surf.surfapi.core.api.util.freeze
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap

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


    internal enum class BlockRow(
        val glyph: Char,
        val row: Int
    ) {
        ONE('ꐱ', 1),
        TWO('ꐲ', 2),
        THREE('ꐳ', 3),
        FOUR('ꐴ', 4),
        FIVE('ꐵ', 5),
        SIX('ꐶ', 6);

        companion object {
            private val index = entries.associateByTo(Int2ObjectOpenHashMap(6)) { it.row }.freeze()
            fun fromRow(row: Int): BlockRow? = index[row]
        }
    }
}