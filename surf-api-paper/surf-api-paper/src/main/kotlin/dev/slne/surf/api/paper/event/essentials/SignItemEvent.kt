package dev.slne.surf.api.paper.event.essentials

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Fired when a player signs an item.
 *
 * This synchronous event is triggered when a player signs an item that they have in their inventory.
 *
 * @property player The player who is signing the item
 * @property item The item being signed
 * @property description The description being written on the item
 */
data class SignItemEvent(
    val player: Player,
    val item: ItemStack,
    val description: String
) : SurfSyncEvent()
