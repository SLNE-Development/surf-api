package dev.slne.surf.api.paper.event.clan

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.OfflinePlayer

data class ClanMemberCountChangeEvent(
    val player: OfflinePlayer,
    val clanName: String,
    val newMemberCount: Int
) : SurfSyncEvent()
