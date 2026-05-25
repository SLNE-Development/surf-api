package dev.slne.surf.api.paper.event.chat

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player
import java.util.*

/**
 * Fired when a player sends a direct message to another player.
 *
 * This synchronous event is triggered when a player sends a private message to another player.
 *
 * @property player The player who sent the direct message
 * @property targetUuid The UUID of the player receiving the direct message
 * @property message The direct message content
 */
data class ChatDirectMessageEvent(
    val player: Player,
    val targetUuid: UUID,
    val message: String
) : SurfSyncEvent()
