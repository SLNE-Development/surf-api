package dev.slne.surf.api.paper.event.freebuild.event

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

/**
 * Fired when a player participates in a bad fishing event.
 *
 * This synchronous event is triggered when a player takes part in a fishing event
 * that results in failure or a negative outcome.
 *
 * @property player The player who participated in the bad fishing event
 */
data class TakePartInBadFishingEventEvent(
    val player: Player
) : SurfSyncEvent()