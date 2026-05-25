package dev.slne.surf.api.paper.event.spawntrader

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.math.BigDecimal

/**
 * Fired when a player completes a trade with a spawn trader.
 *
 * This synchronous event is triggered when a player successfully trades with a spawn trader NPC,
 * exchanging items or currency for goods.
 *
 * @property player The player who completed the trade
 * @property tradeName The name or identifier of the trade/NPC
 * @property itemStack The item involved in the trade
 * @property price The price or value of the trade
 */
data class SpawnTraderTradeEvent(
    val player: Player,
    val tradeName: String,
    val itemStack: ItemStack,
    val price: BigDecimal
) : SurfSyncEvent()
