package dev.slne.surf.surfapi.bukkit.api.inventory.item

import com.github.stefvanschie.inventoryframework.gui.GuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.SinglePlayerGui
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

class SurfGuiItem : GuiItem {

    constructor(item: ItemStack?) : super(item ?: ItemStack.empty())
    constructor() : super(ItemStack.empty())

    var click: InventoryClickEvent.() -> Unit = {}
        set(value) = setAction(value)

    var itemPermission: String? = null
        private set

    var condition: () -> Boolean = { true }

    fun SinglePlayerGui.permission(permission: String) {
        itemPermission = permission
    }
}