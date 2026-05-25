package dev.slne.surf.api.paper.event.freebuild

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

data class ProtectItemEvent(
    val player: Player,
    val itemStack: ItemStack
) : SurfSyncEvent()
