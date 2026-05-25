package dev.slne.surf.api.paper.event.transaction

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player
import java.math.BigDecimal
import java.util.*

/**
 * Fired when a player pays another player.
 *
 * This synchronous event is triggered when a payment transaction occurs between two players.
 *
 * @property player The player making the payment
 * @property targetUuid The UUID of the player receiving the payment
 * @property amount The amount of currency being transferred
 */
data class PlayerPayEvent(
    val player: Player,
    val targetUuid: UUID,
    val amount: BigDecimal
) : SurfSyncEvent()
