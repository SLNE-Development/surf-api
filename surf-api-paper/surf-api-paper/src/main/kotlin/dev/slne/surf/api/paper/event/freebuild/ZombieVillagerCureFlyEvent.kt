package dev.slne.surf.api.paper.event.freebuild

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

data class ZombieVillagerCureFlyEvent(
    val player: Player
) : SurfSyncEvent()
