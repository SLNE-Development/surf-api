package dev.slne.surf.api.paper.event.clan

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.OfflinePlayer

data class ClanInviteCountChangeEvent(
    val player: OfflinePlayer,
    val clanName: String,
    val newInviteCount: Int
) : SurfSyncEvent()
