package dev.slne.surf.surfapi.bukkit.api.gui.context

import dev.slne.surf.surfapi.bukkit.api.gui.GuiItem
import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import dev.slne.surf.surfapi.bukkit.api.gui.component.Component
import dev.slne.surf.surfapi.bukkit.api.gui.view.ViewConfig

/**
 * Context for render operations.
 */
interface InitializeContext {
    /**
     * Get the view configuration.
     */
    fun config(): ViewConfig

    /**
     * Render a component.
     * The component contains its own slot information (startSlot/endSlot).
     */
    fun renderComponent(component: Component)
    
    /**
     * Clear a slot.
     */
    fun clearSlot(slot: Slot)
    
    /**
     * Set an item at a slot without a component.
     */
    fun setItem(slot: Slot, item: GuiItem)
}
