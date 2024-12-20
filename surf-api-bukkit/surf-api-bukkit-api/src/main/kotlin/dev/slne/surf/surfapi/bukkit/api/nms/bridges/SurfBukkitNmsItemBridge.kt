package dev.slne.surf.surfapi.bukkit.api.nms.bridges

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.core.api.util.requiredService
import org.bukkit.inventory.ItemType
import org.jetbrains.annotations.Range

@NmsUseWithCaution
interface SurfBukkitNmsItemBridge {
    fun setDefaultMaxStackSize(item: ItemType, maxStackSize: @Range(from = 1, to = 100) Int)

    companion object {
        @JvmStatic
        val instance = requiredService<SurfBukkitNmsItemBridge>()
    }
}

@NmsUseWithCaution
val itemBridge get() = SurfBukkitNmsItemBridge.instance