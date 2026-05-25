package dev.slne.surf.api.paper.event.chat

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

/**
 * Fired when a player's chat message is blocked due to slow chat rate limiting.
 *
 * This synchronous event is triggered when a player attempts to send a chat message,
 * but the message is blocked because the player is sending messages too quickly (slow chat cooldown).
 *
 * @property player The player whose message was blocked
 * @property message The message that was blocked
 */
data class SlowChatBlockMessageEvent(
    val player: Player,
    val message: String
) : SurfSyncEvent()
