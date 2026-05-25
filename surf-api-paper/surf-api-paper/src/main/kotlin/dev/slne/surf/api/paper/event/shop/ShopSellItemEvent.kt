package dev.slne.surf.api.paper.event.shop

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.ItemStack

data class ShopSellItemEvent(
    val player: OfflinePlayer,
    val itemStack: ItemStack,
    val revenue: Double
) : SurfSyncEvent()