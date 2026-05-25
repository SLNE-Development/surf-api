package dev.slne.surf.api.paper.event.freebuild

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Fired when a player protects an item.
 *
 * This synchronous event is triggered when a player protects an item, preventing it from being
 * dropped, traded, or destroyed.
 *
 * @property player The player who is protecting the item
 * @property itemStack The item being protected
 */
data class ProtectItemEvent(
    val player: Player,
    val itemStack: ItemStack
) : SurfSyncEvent()
