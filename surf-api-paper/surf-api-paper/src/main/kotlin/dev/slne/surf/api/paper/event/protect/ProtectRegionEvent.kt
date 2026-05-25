package dev.slne.surf.api.paper.event.protect

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

/**
 * Fired when a player creates or claims a protected region.
 *
 * This synchronous event is triggered when a player creates a new protected region
 * to protect their buildings and property.
 *
 * @property player The player who created the protected region
 * @property size The size of the newly protected region
 */
data class ProtectRegionEvent(
    val player: Player,
    val size: Int
) : SurfSyncEvent()
