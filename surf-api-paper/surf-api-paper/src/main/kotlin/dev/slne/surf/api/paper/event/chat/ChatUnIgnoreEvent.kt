package dev.slne.surf.api.paper.event.chat

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player
import java.util.*

/**
 * Fired when a player stops ignoring another player.
 *
 * This synchronous event is triggered when a player removes another player from their ignore list,
 * allowing them to receive messages from that player again.
 *
 * @property player The player who is removing someone from their ignore list
 * @property ignoredPlayerUuid The UUID of the player being un-ignored
 */
data class ChatUnIgnoreEvent(
    val player: Player,
    val ignoredPlayerUuid: UUID
) : SurfSyncEvent()
