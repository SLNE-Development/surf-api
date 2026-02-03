package dev.slne.surf.surfapi.bukkit.api.gui.context

import dev.slne.surf.surfapi.bukkit.api.gui.GuiItem
import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import dev.slne.surf.surfapi.bukkit.api.gui.component.Component
import dev.slne.surf.surfapi.bukkit.api.gui.toGuiItem
import org.bukkit.event.inventory.InventoryClickEvent

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
    val item: GuiItem?
        get() = event.currentItem?.toGuiItem()
    
    /**
     * The slot that was clicked.
     */
    val slot: Slot
        get() = Slot.of(event.slot)
    
    /**
     * The component that was clicked, if any.
     */
    val component: Component?
}
