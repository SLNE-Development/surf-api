package dev.slne.surf.api.paper.event.playtime

import dev.slne.surf.api.core.event.SurfAsyncEvent
import org.bukkit.entity.Player

/**
 * Fired (asynchronously) when a player has played for an hour.
 *
 * This asynchronous event is triggered when a player reaches a one-hour playtime milestone.
 *
 * @property player The player who reached the one-hour milestone
 * @property totalHour The total number of hours the player has played
 * @property totalHourOnServer The total number of hours the player has played on this specific server
 * @property server The server identifier where this milestone was reached
 */
data class PlayerPlayAHourEvent(
    val player: Player,
    val totalHour: Long,
    val totalHourOnServer: Long,
    val server: String
) : SurfAsyncEvent()
