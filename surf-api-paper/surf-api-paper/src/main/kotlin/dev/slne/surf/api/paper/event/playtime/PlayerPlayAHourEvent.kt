package dev.slne.surf.api.paper.event.playtime

import dev.slne.surf.api.core.event.SurfAsyncEvent
import org.bukkit.entity.Player

data class PlayerPlayAHourEvent(
    val player: Player,
    val totalHour: Long,
    val totalHourOnServer: Long,
    val server: String
) : SurfAsyncEvent()
