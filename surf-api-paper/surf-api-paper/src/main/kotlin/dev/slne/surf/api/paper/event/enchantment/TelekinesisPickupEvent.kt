package dev.slne.surf.api.paper.event.enchantment

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

/**
 * Fired when a player picks up an item using the Telekinesis enchantment.
 *
 * This synchronous event is triggered when an item is automatically collected by a player
 * due to the Telekinesis enchantment on their tool or weapon.
 *
 * @property player The player who picked up the item with Telekinesis
 */
data class TelekinesisPickupEvent(
    val player: Player
) : SurfSyncEvent()
