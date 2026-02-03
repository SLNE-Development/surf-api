package dev.slne.surf.surfapi.bukkit.server.inventory.manager

import dev.slne.surf.surfapi.bukkit.api.inventory.component.GuiComponent
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages open inventories and their associated GUI components.
 * This is Folia-safe using concurrent data structures.
 */
object InventoryManager {
    private val playerGuis = ConcurrentHashMap<Player, GuiComponent>()
    private val inventoryToGui = ConcurrentHashMap<Inventory, GuiComponent>()

    /**
     * Registers a GUI as opened for a player.
     */
    fun registerGui(player: Player, gui: GuiComponent) {
        playerGuis[player] = gui
        gui.getInventory(player)?.let { inventory ->
            inventoryToGui[inventory] = gui
        }
    }

    /**
     * Unregisters a GUI for a player.
     */
    fun unregisterGui(player: Player) {
        val gui = playerGuis.remove(player)
        gui?.getInventory(player)?.let { inventory ->
            inventoryToGui.remove(inventory)
        }
    }

    /**
     * Gets the GUI component for a player.
     */
    fun getGui(player: Player): GuiComponent? {
        return playerGuis[player]
    }

    /**
     * Gets the GUI component for an inventory.
     */
    fun getGui(inventory: Inventory): GuiComponent? {
        return inventoryToGui[inventory]
    }

    /**
     * Checks if a player has a GUI open.
     */
    fun hasGui(player: Player): Boolean {
        return playerGuis.containsKey(player)
    }

    /**
     * Clears all registered GUIs.
     */
    fun clear() {
        playerGuis.clear()
        inventoryToGui.clear()
    }
}
