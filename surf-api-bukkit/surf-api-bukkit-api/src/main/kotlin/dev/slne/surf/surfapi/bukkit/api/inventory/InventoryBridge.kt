package dev.slne.surf.surfapi.bukkit.api.inventory

import dev.slne.surf.surfapi.bukkit.api.inventory.gui.Gui
import dev.slne.surf.surfapi.bukkit.api.inventory.item.GuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.item.UpdatableGuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.utils.Mask
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.utils.Pattern
import dev.slne.surf.surfapi.core.api.util.InternalSurfApi
import dev.slne.surf.surfapi.core.api.util.requiredService
import org.bukkit.NamespacedKey
import org.bukkit.inventory.Inventory
import java.util.*

@InternalSurfApi
interface InventoryBridge {

    fun getGuiByInventory(inventory: Inventory): Gui?
    fun createGuiItem(key: NamespacedKey?, uuid: UUID?, init: GuiItem.() -> Unit): GuiItem
    fun createUpdatableGuiItem(
        key: NamespacedKey?,
        uuid: UUID?,
        init: UpdatableGuiItem.() -> Unit,
    ): UpdatableGuiItem

    fun createMask(vararg mask: String): Mask
    fun createPattern(vararg pattern: String): Pattern

    companion object {
        val instance = requiredService<InventoryBridge>()
    }
}