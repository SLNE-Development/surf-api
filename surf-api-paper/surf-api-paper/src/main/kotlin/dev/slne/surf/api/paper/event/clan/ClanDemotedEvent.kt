package dev.slne.surf.api.paper.event.clan

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.OfflinePlayer

data class ClanDemotedEvent(
    val player: OfflinePlayer,
    val clanName: String,
    val newRole: String
) : SurfSyncEvent()
