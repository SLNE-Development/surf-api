package dev.slne.surf.api.paper.event.shop

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.Location
import org.bukkit.entity.Player

data class ShopChestPlaceEvent(
    val player: Player,
    val location: Location
) : SurfSyncEvent()