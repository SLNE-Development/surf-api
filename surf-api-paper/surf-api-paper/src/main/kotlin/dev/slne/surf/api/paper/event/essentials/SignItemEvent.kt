package dev.slne.surf.api.paper.event.essentials

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

data class SignItemEvent(
    val player: Player,
    val item: ItemStack,
    val description: String
) : SurfSyncEvent()
