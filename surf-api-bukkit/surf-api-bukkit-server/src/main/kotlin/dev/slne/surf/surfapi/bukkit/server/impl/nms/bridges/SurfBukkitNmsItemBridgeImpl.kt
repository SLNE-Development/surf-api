package dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.SurfBukkitNmsItemBridge
import dev.slne.surf.surfapi.bukkit.server.nms.nms
import dev.slne.surf.surfapi.bukkit.server.reflection.Reflection
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponents
import org.bukkit.inventory.ItemType

@NmsUseWithCaution
object SurfBukkitNmsItemBridgeImpl : SurfBukkitNmsItemBridge {
    override fun setDefaultMaxStackSize(item: ItemType, maxStackSize: Int) {
        require(maxStackSize in 1..100) { "Max stack size must be between 1 and 100" }

        val nmsItem = item.nms
        val updatedComponents = DataComponentMap.builder()
            .addAll(nmsItem.components())
            .set(DataComponents.MAX_STACK_SIZE, maxStackSize)
            .build()
        Reflection.ITEM_PROXY.setComponents(nmsItem, updatedComponents)
    }
}