package dev.slne.surf.api.paper.event.playtime

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

/**
 * Fired when a player refuses their paycheck payment.
 *
 * This synchronous event is triggered when a player declines or refuses to accept
 * their regular paycheck/salary.
 *
 * @property player The player who refused the paycheck
 */
data class PayCheckRefusePaymentEvent(
    val player: Player
) : SurfSyncEvent()
