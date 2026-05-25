package dev.slne.surf.api.paper.event.freebuild.specialdrops

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Fired when a player receives a special drop from archaeology.
 *
 * This synchronous event is triggered when a player obtains a special/rare item drop
 * from archaeology activities.
 *
 * @property player The player who received the special archaeology drop
 * @property itemStack The special item that was dropped
 */
data class ArchaeologySpecialDropEvent(
    val player: Player,
    val itemStack: ItemStack
) : SurfSyncEvent()
