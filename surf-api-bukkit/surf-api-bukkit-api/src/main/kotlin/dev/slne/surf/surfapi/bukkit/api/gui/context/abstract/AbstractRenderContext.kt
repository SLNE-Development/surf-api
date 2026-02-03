package dev.slne.surf.surfapi.bukkit.api.gui.context.abstract

import dev.slne.surf.surfapi.bukkit.api.gui.GuiItem
import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import dev.slne.surf.surfapi.bukkit.api.gui.component.Component
import dev.slne.surf.surfapi.bukkit.api.gui.context.RenderContext
import dev.slne.surf.surfapi.bukkit.api.gui.toItemStack
import dev.slne.surf.surfapi.bukkit.api.gui.view.AbstractGuiView
import dev.slne.surf.surfapi.bukkit.api.gui.view.GuiView
import dev.slne.surf.surfapi.core.api.util.InternalSurfApi
import org.bukkit.entity.Player

@InternalSurfApi
class AbstractRenderContext(
    override val view: GuiView,
    override val player: Player,
    private val bukkitView: AbstractGuiView
) : RenderContext {
    override fun renderComponent(component: Component) {
        bukkitView.addComponent(component)
    }

    override fun clearSlot(slot: Slot) {
        // Find and remove all components that occupy this slot
        val componentsToRemove = bukkitView.findComponentsBySlot(slot)
        componentsToRemove.forEach { component ->
            bukkitView.removeComponent(component)
        }
    }

    override fun setItem(slot: Slot, item: GuiItem) {
        val inventory = player.openInventory.topInventory
        if (slot.index in 0 until inventory.size) {
            inventory.setItem(slot.index, item.toItemStack())
        }
    }

    override fun navigateTo(view: GuiView, passProps: Boolean) {
        NavigationHelper.navigateTo(this.view, view, player, passProps)
    }

    override fun navigateBack() {
        NavigationHelper.navigateBack(view, player)
    }

    override fun close() {
        NavigationHelper.close(player)
    }

    override fun update() {
        NavigationHelper.update(view, player)
    }
}