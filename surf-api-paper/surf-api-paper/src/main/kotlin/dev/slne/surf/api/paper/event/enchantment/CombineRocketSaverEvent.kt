package dev.slne.surf.api.paper.event.enchantment

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

data class CombineRocketSaverEvent(
    val player: Player
) : SurfSyncEvent()
