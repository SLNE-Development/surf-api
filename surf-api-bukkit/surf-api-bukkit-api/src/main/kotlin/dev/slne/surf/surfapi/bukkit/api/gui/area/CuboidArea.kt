package dev.slne.surf.surfapi.bukkit.api.gui.area

import dev.slne.surf.surfapi.bukkit.api.gui.Slot

/**
 * A rectangular (cuboid) area defined by start and end slots.
 * This is the most common area type for panels, grids, and paginated content.
 */
data class CuboidArea(
    val startSlot: Slot,
    val endSlot: Slot
) : ComponentArea {
    override fun slots(): Set<Slot> {
        val slots = mutableSetOf<Slot>()
        for (row in startSlot.row..endSlot.row) {
            for (col in startSlot.column..endSlot.column) {
                slots.add(Slot.at(col, row))
            }
        }
        return slots
    }

    override fun contains(slot: Slot): Boolean {
        return slot.column >= startSlot.column &&
                slot.column <= endSlot.column &&
                slot.row >= startSlot.row &&
                slot.row <= endSlot.row
    }

    override fun toString(): String {
        return "CuboidArea(startSlot=$startSlot, endSlot=$endSlot, width=$width, height=$height)"
    }

    override val width: Int
        get() = endSlot.column - startSlot.column + 1

    override val height: Int
        get() = endSlot.row - startSlot.row + 1
}
