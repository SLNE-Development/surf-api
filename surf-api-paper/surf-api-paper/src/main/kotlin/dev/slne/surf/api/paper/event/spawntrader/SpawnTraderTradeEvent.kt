package dev.slne.surf.api.paper.event.spawntrader

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

data class SpawnTraderTradeEvent(
    val player: Player,
    val tradeName: String,
    val itemStack: ItemStack,
    val price: Int
) : SurfSyncEvent()
