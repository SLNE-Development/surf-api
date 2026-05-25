package dev.slne.surf.api.paper.event.stats

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

data class StatsOptoutSettingEvent(
    val player: Player
) : SurfSyncEvent()
