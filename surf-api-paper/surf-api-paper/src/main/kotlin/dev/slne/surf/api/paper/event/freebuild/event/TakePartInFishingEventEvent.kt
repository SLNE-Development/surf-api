package dev.slne.surf.api.paper.event.freebuild.event

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

/**
 * Fired when a player participates in a fishing event.
 *
 * This synchronous event is triggered when a player takes part in a fishing event,
 * typically a limited-time event with special rewards or challenges.
 *
 * @property player The player who participated in the fishing event
 */
data class TakePartInFishingEventEvent(
    val player: Player
) : SurfSyncEvent()