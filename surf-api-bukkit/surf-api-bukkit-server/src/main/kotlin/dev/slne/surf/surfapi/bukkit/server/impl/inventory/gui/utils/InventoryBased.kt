package dev.slne.surf.surfapi.bukkit.server.impl.inventory.gui.utils

import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

interface InventoryBased : InventoryHolder {
    fun createInventory(): Inventory
}