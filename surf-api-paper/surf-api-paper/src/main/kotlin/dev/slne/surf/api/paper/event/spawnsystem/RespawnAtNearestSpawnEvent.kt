package dev.slne.surf.api.paper.event.spawnsystem

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.Location
import org.bukkit.entity.Player

data class RespawnAtNearestSpawnEvent(
    val player: Player,
    val respawnLocation: Location
) : SurfSyncEvent()
