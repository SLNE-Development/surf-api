package dev.slne.surf.surfapi.bukkit.api.nms.bridges

import dev.slne.surf.surfapi.bukkit.api.nms.SurfBukkitNmsBridge
import org.bukkit.inventory.ItemType
import org.jetbrains.annotations.ApiStatus.NonExtendable
import org.jetbrains.annotations.Range

@NonExtendable
interface SurfBukkitNmsItemBridge {
    fun setDefaultMaxStackSize(item: ItemType, maxStackSize: @Range(from = 1, to = 100) Int)

    companion object {
        @JvmStatic
        fun get(): SurfBukkitNmsItemBridge = SurfBukkitNmsBridge.get().itemBridge
    }
}

val itemBridge get() = SurfBukkitNmsItemBridge.get()