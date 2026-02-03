package dev.slne.surf.surfapi.bukkit.server.inventory.listener

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.surfapi.bukkit.api.inventory.component.ItemComponent
import dev.slne.surf.surfapi.bukkit.server.inventory.manager.InventoryManager
import dev.slne.surf.surfapi.bukkit.server.plugin
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent

/**
 * Listener for inventory events.
 * Handles clicks and manages GUI lifecycle in a Folia-safe manner.
 */
class InventoryListener : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val player = event.player as? Player ?: return
        val gui = InventoryManager.getGui(event.inventory) ?: return

        // Register the GUI with the manager
        InventoryManager.registerGui(player, gui)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        val gui = InventoryManager.getGui(player) ?: return

        // Unregister the GUI
        InventoryManager.unregisterGui(player)

        // Call onUnmount in a Folia-safe manner
        plugin.launch(player) {
            gui.onUnmount()
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val gui = InventoryManager.getGui(player) ?: return

        val slot = event.rawSlot

        // Find the item component at this slot
        val itemComponent = gui.children
            .filterIsInstance<ItemComponent>()
            .firstOrNull { it.slot == slot }

        if (itemComponent != null) {
            // Cancel the event if the item cannot be taken
            if (!itemComponent.canTake) {
                event.isCancelled = true
            }

            // Call the click handler in a Folia-safe manner
            plugin.launch(player) {
                itemComponent.onClick(player, event.click)
            }
        } else {
            // No item component at this slot, cancel by default
            event.isCancelled = true
        }
    }
}
