package dev.slne.surf.api.paper.event.essentials

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

data class CommandFailEvent(
    val player: Player,
    val command: String
) : SurfSyncEvent()
