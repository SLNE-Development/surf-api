package dev.slne.surf.surfapi.bukkit.api.inventory.item

import dev.slne.surf.surfapi.bukkit.api.inventory.InventoryBridge
import dev.slne.surf.surfapi.bukkit.api.inventory.dsl.GuiItemDsl
import dev.slne.surf.surfapi.bukkit.api.inventory.gui.handlers.ClickHandler
import dev.slne.surf.surfapi.bukkit.api.inventory.gui.handlers.ClickHandlerDsl
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import java.util.*

@GuiItemDsl
interface GuiItem : Cloneable {
    val itemStack: ItemStack
    val key: NamespacedKey
    val uuid: UUID
    var visible: Boolean

    fun item(itemStack: ItemStack)
    fun item(type: ItemType, block: (@GuiItemDsl ItemStack).() -> Unit = {})

    fun onClick(handler: @GuiItemDsl ClickHandler)
    fun onClick(handler: @GuiItemDsl ClickHandlerDsl)

    public override fun clone(): GuiItem = super.clone() as GuiItem

    companion object {
        operator fun invoke(
            key: NamespacedKey? = null,
            uuid: UUID? = null,
            init: GuiItem.() -> Unit,
        ): GuiItem = InventoryBridge.instance.createGuiItem(key, uuid, init)
    }
}