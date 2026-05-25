package dev.slne.surf.api.paper.event.chat

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player
import java.util.*

data class ChatIgnoreEvent(
    val player: Player,
    val ignoredPlayerUuid: UUID
) : SurfSyncEvent()
