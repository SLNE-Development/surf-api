@file:JvmName("InventoryExtensions")

package dev.slne.surf.surfapi.bukkit.api.inventory

import dev.slne.surf.surfapi.bukkit.api.inventory.component.GuiComponent
import org.bukkit.entity.Player

/**
 * Opens a GUI for the player.
 */
suspend fun Player.openGui(gui: GuiComponent) {
    gui.onMount()
    gui.open(this)
}
