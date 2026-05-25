package dev.slne.surf.api.paper.event.freebuild.event

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

data class TakePartInBadFishingEventEvent(
    val player: Player
) : SurfSyncEvent()