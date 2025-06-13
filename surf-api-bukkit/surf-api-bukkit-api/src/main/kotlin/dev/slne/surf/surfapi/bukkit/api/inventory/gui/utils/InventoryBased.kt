package dev.slne.surf.surfapi.bukkit.api.inventory.gui.utils

import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

internal interface InventoryBased : InventoryHolder {

    fun createInventory(): Inventory

}