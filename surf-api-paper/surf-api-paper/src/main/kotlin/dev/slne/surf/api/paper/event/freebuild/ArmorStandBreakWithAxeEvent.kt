package dev.slne.surf.api.paper.event.freebuild

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

/**
 * Fired when a player breaks an armor stand with an axe.
 *
 * This synchronous event is triggered when a player uses an axe to break or destroy an armor stand.
 *
 * @property player The player who broke the armor stand
 */
data class ArmorStandBreakWithAxeEvent(
    val player: Player
) : SurfSyncEvent()
