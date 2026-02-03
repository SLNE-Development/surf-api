package dev.slne.surf.surfapi.bukkit.api.gui.context

import dev.slne.surf.surfapi.bukkit.api.gui.component.Component
import org.bukkit.inventory.ItemStack

/**
 * Context for render operations.
 */
interface RenderContext : ViewContext {
    /**
     * Render a component at the specified slot.
     */
    fun renderComponent(slot: Int, component: Component)
    
    /**
     * Clear a slot.
     */
    fun clearSlot(slot: Int)
    
    /**
     * Set an item at a slot without a component.
     */
    fun setItem(slot: Int, item: ItemStack)
}
