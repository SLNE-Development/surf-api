package dev.slne.surf.surfapi.bukkit.api.gui.context.abstract

import dev.slne.surf.surfapi.bukkit.api.gui.GuiItem
import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import dev.slne.surf.surfapi.bukkit.api.gui.component.Component
import dev.slne.surf.surfapi.bukkit.api.gui.context.InitializeContext
import dev.slne.surf.surfapi.bukkit.api.gui.toItemStack
import dev.slne.surf.surfapi.bukkit.api.gui.view.GuiView
import dev.slne.surf.surfapi.bukkit.api.gui.view.ViewConfig
import dev.slne.surf.surfapi.shared.api.util.InternalSurfApi

@InternalSurfApi
class AbstractInitializeContext(
    val config: ViewConfig,
    val view: GuiView
) : InitializeContext {
    override fun config(): ViewConfig {
        return config
    }

    override fun renderComponent(component: Component) {
        view.addComponent(component)
    }

    override fun clearSlot(slot: Slot) {
        // Find and remove all components that occupy this slot
        val componentsToRemove = view.findComponentsBySlot(slot)
        componentsToRemove.forEach { component ->
            view.removeComponent(component)
        }
    }

    override fun setItem(slot: Slot, item: GuiItem) {
        val inventory = view.inventory
        
        if (slot.index in 0 until inventory.size) {
            inventory.setItem(slot.index, item.toItemStack())
        }
    }
}