package dev.slne.surf.surfapi.bukkit.api.inventory.item

import org.bukkit.NamespacedKey
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import java.util.*

class UpdatableGuiItem(
    itemStack: ItemStack,
    action: (InventoryClickEvent) -> Unit = {},
    internal var update: (UpdatableGuiItem) -> Boolean = { false },
    key: NamespacedKey = GUI_ITEM_UUID_KEY,
    uuid: UUID = UUID.randomUUID(),
) : GuiItem(itemStack, action, key, uuid) {
    fun onUpdate(update: () -> Boolean) {
        this.update = { update() }
    }
}