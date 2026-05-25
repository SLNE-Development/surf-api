package dev.slne.surf.api.paper.event.enchantment

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

/**
 * Fired when a player replenishes a crop using the Replenish enchantment.
 *
 * This synchronous event is triggered when a crop is automatically regrown or replanted
 * due to the Replenish enchantment on a player's tool.
 *
 * @property player The player who triggered the crop replenishment
 */
data class ReplenishCropEvent(
    val player: Player
) : SurfSyncEvent()
