package dev.slne.surf.surfapi.bukkit.server.impl.inventory.item

import dev.slne.surf.surfapi.bukkit.api.inventory.dsl.GuiItemDsl
import dev.slne.surf.surfapi.bukkit.api.inventory.item.UpdatableGuiItem
import org.bukkit.NamespacedKey
import java.util.*
import java.util.function.BooleanSupplier

class UpdatableGuiItemImpl(
    key: NamespacedKey = DEFAULT_KEY,
    uuid: UUID = UUID.randomUUID(),
) : GuiItemImpl(key, uuid), UpdatableGuiItem {
    private var update: BooleanSupplier? = null

    override fun onUpdate(update: @GuiItemDsl BooleanSupplier) {
        this.update = update
    }

    fun update(): Boolean = update?.asBoolean == true
}