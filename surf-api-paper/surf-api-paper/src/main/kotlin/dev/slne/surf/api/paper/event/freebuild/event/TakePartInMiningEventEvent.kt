package dev.slne.surf.api.paper.event.freebuild.event

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

/**
 * Fired when a player participates in a mining event.
 *
 * This synchronous event is triggered when a player takes part in a mining event,
 * typically a limited-time event with special rewards or challenges.
 *
 * @property player The player who participated in the mining event
 */
data class TakePartInMiningEventEvent(
    val player: Player
) : SurfSyncEvent()