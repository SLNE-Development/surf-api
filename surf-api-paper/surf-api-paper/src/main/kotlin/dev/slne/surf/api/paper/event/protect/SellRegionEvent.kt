package dev.slne.surf.api.paper.event.protect

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

data class SellRegionEvent(
    val player: Player,
    val size: Int,
    val receivedMoney: Double
) : SurfSyncEvent()