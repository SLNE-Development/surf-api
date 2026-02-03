package dev.slne.surf.surfapi.bukkit.api.gui.area

import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import dev.slne.surf.surfapi.bukkit.api.gui.component.ComponentPriority

/**
 * An area consisting of a single slot.
 * Used for simple components like buttons or single items.
 */
data class SingleSlotArea(
    val slot: Slot,
    override val priority: ComponentPriority = ComponentPriority.NORMAL
) : ComponentArea {
    override fun slots(): Set<Slot> = setOf(slot)
    
    override fun contains(slot: Slot): Boolean = this.slot == slot
    
    override val width: Int = 1
    override val height: Int = 1
}
