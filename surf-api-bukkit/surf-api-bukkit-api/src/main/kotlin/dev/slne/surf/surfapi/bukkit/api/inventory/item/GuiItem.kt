package dev.slne.surf.surfapi.bukkit.api.inventory.item

import com.github.stefvanschie.inventoryframework.gui.GuiItem
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

fun guiItem(item: ItemStack, action: InventoryClickEvent.() -> Unit = {}) = GuiItem(item, action)
fun guiItem(
    material: Material,
    item: ItemStack.() -> Unit,
    action: InventoryClickEvent.() -> Unit = {}
) = GuiItem(ItemStack(material).apply(item), action)