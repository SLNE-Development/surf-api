package dev.slne.surf.api.paper.event.essentials

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Fired when a player receives a signed item.
 *
 * This synchronous event is triggered when a player receives an item that has been signed
 * by another player with a description and the signer's name.
 *
 * @property player The player who received the signed item
 * @property item The signed item
 * @property description The description written on the item
 * @property signedByName The name of the player who signed the item
 */
data class SignItemReceiveEvent(
    val player: Player,
    val item: ItemStack,
    val description: String,
    val signedByName: String
) : SurfSyncEvent()
