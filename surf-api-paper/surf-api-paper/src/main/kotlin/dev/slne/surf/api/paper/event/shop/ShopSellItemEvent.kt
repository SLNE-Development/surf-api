package dev.slne.surf.api.paper.event.shop

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.ItemStack
import java.math.BigDecimal

/**
 * Fired when a player sells an item to a shop.
 *
 * This synchronous event is triggered when a player sells an item to a shop,
 * receiving currency in exchange for goods.
 *
 * @property player The player who sold the item
 * @property itemStack The item that was sold
 * @property revenue The amount of money received from the sale
 */
data class ShopSellItemEvent(
    val player: OfflinePlayer,
    val itemStack: ItemStack,
    val revenue: BigDecimal
) : SurfSyncEvent()