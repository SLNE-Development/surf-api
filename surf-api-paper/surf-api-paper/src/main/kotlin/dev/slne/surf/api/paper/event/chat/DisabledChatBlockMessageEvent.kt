package dev.slne.surf.api.paper.event.chat

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

data class DisabledChatBlockMessageEvent(
    val player: Player,
    val message: String
) : SurfSyncEvent()
