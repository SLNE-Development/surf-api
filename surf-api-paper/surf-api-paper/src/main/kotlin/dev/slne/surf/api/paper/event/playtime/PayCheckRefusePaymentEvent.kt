package dev.slne.surf.api.paper.event.playtime

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

data class PayCheckRefusePaymentEvent(
    val player: Player
) : SurfSyncEvent()
