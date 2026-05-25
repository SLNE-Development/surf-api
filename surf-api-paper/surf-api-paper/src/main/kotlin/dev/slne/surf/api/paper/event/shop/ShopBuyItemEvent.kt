package dev.slne.surf.api.paper.event.shop

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.math.BigDecimal

/**
 * Fired when a player buys an item from a shop.
 *
 * This synchronous event is triggered when a player purchases an item from a shop,
 * spending currency in exchange for goods.
 *
 * @property player The player who bought the item
 * @property itemStack The item that was purchased
 * @property price The price paid for the item
 */
data class ShopBuyItemEvent(
    val player: Player,
    val itemStack: ItemStack,
    val price: BigDecimal
) : SurfSyncEvent()