package dev.slne.surf.api.paper.event.spawnsystem

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.Location
import org.bukkit.entity.Player

/**
 * Fired when a player respawns at the nearest spawn point.
 *
 * This synchronous event is triggered when a player respawns at the nearest available
 * spawn location after dying.
 *
 * @property player The player who respawned
 * @property respawnLocation The location where the player respawned
 */
data class RespawnAtNearestSpawnEvent(
    val player: Player,
    val respawnLocation: Location
) : SurfSyncEvent()
