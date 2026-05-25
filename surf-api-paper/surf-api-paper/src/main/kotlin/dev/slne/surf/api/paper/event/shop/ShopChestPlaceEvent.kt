package dev.slne.surf.api.paper.event.shop

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.Location
import org.bukkit.entity.Player

/**
 * Fired when a player places a shop chest.
 *
 * This synchronous event is triggered when a player places a chest that functions as a shop,
 * allowing it to sell items to other players.
 *
 * @property player The player who placed the shop chest
 * @property location The location where the shop chest was placed
 */
data class ShopChestPlaceEvent(
    val player: Player,
    val location: Location
) : SurfSyncEvent()