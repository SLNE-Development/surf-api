package dev.slne.surf.surfapi.bukkit.api.inventory.view

import org.bukkit.inventory.InventoryView
import org.bukkit.inventory.ItemStack


object InventoryViewUtil {

    fun getTitle(view: InventoryView) = view.title()

    fun getTopInventory(view: InventoryView) = view.topInventory
    fun getBottomInventory(view: InventoryView) = view.bottomInventory

    fun getCursor(view: InventoryView) = view.cursor
    fun setCursor(view: InventoryView, item: ItemStack?) {
        view.setCursor(item)
    }

    fun getInventory(view: InventoryView, slot: Int) = view.getInventory(slot)
    fun getSlotType(view: InventoryView, slot: Int) = view.getSlotType(slot)
}