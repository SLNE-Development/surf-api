@file:OptIn(InternalSurfApi::class)

package dev.slne.surf.api.paper.nms.bridges

import dev.slne.surf.api.di.surfBean
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import org.jetbrains.annotations.Range

@NmsUseWithCaution
interface SurfPaperNmsItemBridge {
    fun setDefaultMaxStackSize(item: ItemType, maxStackSize: @Range(from = 1, to = 100) Int)

    fun getCreativeSearchItemOrderComparator(): Comparator<ItemStack?>

    companion object {
        val INSTANCE get() = bridge
    }
}

@OptIn(NmsUseWithCaution::class)
private val bridge by surfBean<SurfPaperNmsItemBridge>()
