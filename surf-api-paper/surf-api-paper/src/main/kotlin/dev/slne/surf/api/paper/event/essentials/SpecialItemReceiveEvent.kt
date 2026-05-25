package dev.slne.surf.api.paper.event.essentials

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Fired when a player receives a special item.
 *
 * This synchronous event is triggered whenever a special item is given to a player.
 *
 * @property player The player who received the special item
 * @property item The special item that was received
 */
data class SpecialItemReceiveEvent(
    val player: Player,
    val item: ItemStack
) : SurfSyncEvent()
