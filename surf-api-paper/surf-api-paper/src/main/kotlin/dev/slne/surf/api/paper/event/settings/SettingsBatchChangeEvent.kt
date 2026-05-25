package dev.slne.surf.api.paper.event.settings

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

data class SettingsBatchChangeEvent(
    val player: Player,
    val amount: Int
) : SurfSyncEvent()
