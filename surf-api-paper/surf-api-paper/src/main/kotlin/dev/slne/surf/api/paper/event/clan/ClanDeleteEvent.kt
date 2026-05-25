package dev.slne.surf.api.paper.event.clan

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

data class ClanDeleteEvent(
    val player: Player,
    val clanName: String,
    val clanTag: String
) : SurfSyncEvent()
