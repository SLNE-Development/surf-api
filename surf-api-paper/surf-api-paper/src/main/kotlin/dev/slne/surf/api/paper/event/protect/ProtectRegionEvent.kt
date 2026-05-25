package dev.slne.surf.api.paper.event.protect

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

data class ProtectRegionEvent(
    val player: Player,
    val size: Int
) : SurfSyncEvent()
