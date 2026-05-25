package dev.slne.surf.api.paper.event.transaction

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player
import java.util.*

data class PlayerPayEvent(
    val player: Player,
    val targetUuid: UUID,
    val amount: Int
) : SurfSyncEvent()
