package dev.slne.surf.api.paper.event.protect

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player
import java.math.BigDecimal

/**
 * Fired when a player sells a protected region.
 *
 * This synchronous event is triggered when a player sells off a protected region,
 * receiving monetary compensation for it.
 *
 * @property player The player who sold the region
 * @property size The size of the sold region
 * @property receivedMoney The amount of money received from the sale
 */
data class SellRegionEvent(
    val player: Player,
    val size: Int,
    val receivedMoney: BigDecimal
) : SurfSyncEvent()