package dev.slne.surf.api.paper.event.chat

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player
import java.util.*

/**
 * Fired when a player ignores another player.
 *
 * This synchronous event is triggered when a player adds another player to their ignore list,
 * preventing them from receiving messages from that player.
 *
 * @property player The player who is ignoring someone
 * @property ignoredPlayerUuid The UUID of the player being ignored
 */
data class ChatIgnoreEvent(
    val player: Player,
    val ignoredPlayerUuid: UUID
) : SurfSyncEvent()
