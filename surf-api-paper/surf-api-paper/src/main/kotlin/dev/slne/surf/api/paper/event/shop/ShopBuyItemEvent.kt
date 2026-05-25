package dev.slne.surf.api.paper.event.shop

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

data class ShopBuyItemEvent(
    val player: Player,
    val itemStack: ItemStack,
    val price: Double
) : SurfSyncEvent()