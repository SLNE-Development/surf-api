package dev.slne.surf.api.paper.event.playtime

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

/**
 * Fired when a player receives their paycheck payment.
 *
 * This synchronous event is triggered when a player receives their regular paycheck/salary
 * based on their playtime.
 *
 * @property player The player who received the paycheck
 */
data class PayCheckPayEvent(
    val player: Player
) : SurfSyncEvent()
