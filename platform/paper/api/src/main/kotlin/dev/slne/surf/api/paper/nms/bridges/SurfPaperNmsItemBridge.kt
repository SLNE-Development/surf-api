package dev.slne.surf.api.paper.nms.bridges

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.spring.SurfApiBean
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import org.jetbrains.annotations.Range

@NmsUseWithCaution
@SurfApiBean
interface SurfPaperNmsItemBridge {
    fun setDefaultMaxStackSize(item: ItemType, maxStackSize: @Range(from = 1, to = 100) Int)

    fun getCreativeSearchItemOrderComparator(): Comparator<ItemStack?>
}
