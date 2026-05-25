package dev.slne.surf.api.paper.event.protect

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

data class ExtendRegionEvent(
    val player: Player,
    val size: Int
) : SurfSyncEvent()