package dev.slne.surf.surfapi.bukkit.api.gui.context

import dev.slne.surf.surfapi.bukkit.api.gui.GuiItem
import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import dev.slne.surf.surfapi.bukkit.api.gui.component.Component

/**
 * Context for render operations.
 */
interface RenderContext : ViewContext {
    /**
     * Render a component at the specified slot.
     */
    fun renderComponent(slot: Slot, component: Component)
    
    /**
     * Clear a slot.
     */
    fun clearSlot(slot: Slot)
    
    /**
     * Set an item at a slot without a component.
     */
    fun setItem(slot: Slot, item: GuiItem)
}
