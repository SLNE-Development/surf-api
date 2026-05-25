package dev.slne.surf.api.paper.event.playtime

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

/**
 * Fired when a player has played for a minute.
 *
 * This synchronous event is triggered when a player reaches a one-minute playtime milestone.
 *
 * @property player The player who reached the one-minute milestone
 */
data class PlayerPlayAMinuteEvent(
    val player: Player
) : SurfSyncEvent()
