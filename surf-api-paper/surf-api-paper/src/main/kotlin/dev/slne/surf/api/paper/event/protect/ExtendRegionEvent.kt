package dev.slne.surf.api.paper.event.protect

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

/**
 * Fired when a player extends a protected region.
 *
 * This synchronous event is triggered when a player increases the size of an existing
 * protected region.
 *
 * @property player The player who extended the region
 * @property size The new size of the extended region
 */
data class ExtendRegionEvent(
    val player: Player,
    val size: Int
) : SurfSyncEvent()