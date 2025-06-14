package dev.slne.surf.surfapi.bukkit.api.inventory.utils

import dev.slne.surf.surfapi.bukkit.api.inventory.item.GuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.Pane
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory

interface InventoryComponent : Cloneable {
    val length: Int
    val height: Int

    fun addPane(pane: Pane)
    fun display(inventory: Inventory, offset: Int)
    fun display(playerInventory: PlayerInventory, offset: Int)
    fun placeItems(playerInventory: PlayerInventory, offset: Int)
    fun placeItems(inventory: Inventory, offset: Int)

    fun excludeRows(from: Int, end: Int): InventoryComponent
    fun hasItem(): Boolean
    fun hasItem(slot: Slot): Boolean
    fun getItem(slot: Slot): ItemStack?
    fun setItem(item: GuiItem, slot: Slot)
    fun setItem(itemStack: ItemStack, slot: Slot)
    fun clearItems()
    fun inBounds(slot: Slot): Boolean
    fun inBounds(lowerBound: Int, upperBound: Int, value: Int): Boolean
    fun getPane(index: Int): Pane

    fun display()

    public override fun clone(): InventoryComponent
}