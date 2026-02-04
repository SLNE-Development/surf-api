package dev.slne.surf.surfapi.bukkit.api.gui.area

import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import dev.slne.surf.surfapi.core.api.util.objectSetOf
import it.unimi.dsi.fastutil.objects.ObjectSet

/**
 * An area consisting of a single slot.
 * Used for simple components like buttons or single items.
 */
data class SingleSlotArea(
    val slot: Slot
) : ComponentArea {
    override fun slots(): ObjectSet<Slot> = objectSetOf(slot)

    override fun contains(slot: Slot): Boolean = this.slot == slot

    override fun toString(): String {
        return "SingleSlotArea(slot=$slot, width=$width, height=$height)"
    }

    override val width: Int = 1
    override val height: Int = 1
}
