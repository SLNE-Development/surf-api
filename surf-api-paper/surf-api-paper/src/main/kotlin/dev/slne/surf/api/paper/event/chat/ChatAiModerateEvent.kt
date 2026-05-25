package dev.slne.surf.api.paper.event.chat

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

/**
 * Fired when a chat message is moderated by AI.
 *
 * This synchronous event is triggered when an AI moderation system processes and filters a player's chat message.
 *
 * @property player The player who wrote the chat message
 * @property message The chat message that was moderated
 */
data class ChatAiModerateEvent(
    val player: Player,
    val message: String
) : SurfSyncEvent()
