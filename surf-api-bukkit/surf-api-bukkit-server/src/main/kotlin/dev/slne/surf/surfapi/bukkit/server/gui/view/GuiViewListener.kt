package dev.slne.surf.surfapi.bukkit.server.gui.view

import dev.slne.surf.surfapi.bukkit.api.gui.view.AbstractGuiView
import dev.slne.surf.surfapi.bukkit.api.gui.view.ViewManager
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent

/**
 * Global event listener for all GUI views.
 */
object GuiViewListener : Listener {
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val view = ViewManager.getActiveView(player) ?: return

        if (view is AbstractGuiView) {
            view.handleClick(player, event)
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        val view = ViewManager.getActiveView(player) ?: return
        view.close(player)
    }
}