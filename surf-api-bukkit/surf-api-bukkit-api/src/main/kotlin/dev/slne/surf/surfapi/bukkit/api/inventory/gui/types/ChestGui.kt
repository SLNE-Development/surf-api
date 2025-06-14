package dev.slne.surf.surfapi.bukkit.api.inventory.gui.types

import dev.slne.surf.surfapi.bukkit.api.inventory.gui.MergedGui
import dev.slne.surf.surfapi.bukkit.api.inventory.gui.NamedGui

interface ChestGui: NamedGui, MergedGui {
    val size: ChestGuiSize
    fun size(size: ChestGuiSize)

    override fun clone(): ChestGui

    enum class ChestGuiSize(val rows: Int) {
        ONE_ROW(1),
        TWO_ROWS(2),
        THREE_ROWS(3),
        FOUR_ROWS(4),
        FIVE_ROWS(5),
        SIX_ROWS(6);
    }
}