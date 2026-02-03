package dev.slne.surf.surfapi.bukkit.api.gui.context

import dev.slne.surf.surfapi.bukkit.api.gui.component.Component
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

/**
 * Context for click events.
 */
interface ClickContext : ViewContext {
    /**
     * The click event.
     */
    val event: InventoryClickEvent
    
    /**
     * The clicked item.
     */
    val item: ItemStack?
        get() = event.currentItem
    
    /**
     * The slot that was clicked.
     */
    val slot: Int
        get() = event.slot
    
    /**
     * The component that was clicked, if any.
     */
    val component: Component?
}
