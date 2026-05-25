package dev.slne.surf.api.paper.event.enchantment

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

/**
 * Fired when a player combines items with the Mending enchantment.
 *
 * This synchronous event is triggered when a player combines items in an anvil or crafting
 * interface, resulting in an item with the Mending enchantment applied.
 *
 * @property player The player who combined the items
 */
data class CombineMendingEvent(
    val player: Player
) : SurfSyncEvent()
