package dev.slne.surf.api.paper.event.freebuild.specialdrops

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Fired when a player receives a special drop from fishing.
 *
 * This synchronous event is triggered when a player catches a special/rare item drop
 * from fishing activities.
 *
 * @property player The player who received the special fishing drop
 * @property itemStack The special item that was caught
 */
data class FishingSpecialDropEvent(
    val player: Player,
    val itemStack: ItemStack
) : SurfSyncEvent()
