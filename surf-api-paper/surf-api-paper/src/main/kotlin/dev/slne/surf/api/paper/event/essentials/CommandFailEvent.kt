package dev.slne.surf.api.paper.event.essentials

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

/**
 * Fired when a command execution fails for a player.
 *
 * This synchronous event is triggered when a command issued by a player fails to execute.
 *
 * @property player The player who executed the failed command
 * @property command The command that failed to execute
 */
data class CommandFailEvent(
    val player: Player,
    val command: String
) : SurfSyncEvent()
