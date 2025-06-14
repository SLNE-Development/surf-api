package dev.slne.surf.surfapi.bukkit.api.inventory.item

import dev.slne.surf.surfapi.bukkit.api.inventory.InventoryBridge
import dev.slne.surf.surfapi.bukkit.api.inventory.dsl.GuiItemDsl
import org.bukkit.NamespacedKey
import java.util.*
import java.util.function.BooleanSupplier

@GuiItemDsl
interface UpdatableGuiItem : GuiItem {

    fun onUpdate(update: @GuiItemDsl BooleanSupplier)

    override fun clone(): UpdatableGuiItem = super.clone() as UpdatableGuiItem

    companion object {
        operator fun invoke(
            key: NamespacedKey? = null,
            uuid: UUID? = null,
            init: UpdatableGuiItem.() -> Unit,
        ): UpdatableGuiItem = InventoryBridge.instance.createUpdatableGuiItem(key, uuid, init)
    }
}