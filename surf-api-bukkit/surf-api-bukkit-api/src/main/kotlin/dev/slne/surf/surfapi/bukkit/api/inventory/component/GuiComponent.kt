package dev.slne.surf.surfapi.bukkit.api.inventory.component

import net.kyori.adventure.text.Component as AdventureComponent
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

/**
 * Represents an inventory GUI component.
 * This is the main container for inventory-based UIs.
 */
interface GuiComponent : RenderableComponent {
    /**
     * The title of the GUI.
     */
    val title: AdventureComponent

    /**
     * The number of rows in the GUI (1-6).
     */
    val rows: Int

    /**
     * Opens the GUI for the specified player.
     */
    suspend fun open(player: Player)

    /**
     * Closes the GUI for the specified player.
     */
    suspend fun close(player: Player)

    /**
     * Gets the Bukkit inventory for the specified player.
     */
    fun getInventory(player: Player): Inventory?

    /**
     * Updates the GUI for the specified player.
     */
    suspend fun updateFor(player: Player)
}
