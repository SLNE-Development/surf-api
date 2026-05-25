package dev.slne.surf.api.paper.event.clan

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.OfflinePlayer

/**
 * Fired when the member count of a clan changes.
 *
 * This synchronous event is triggered when the number of members in a clan changes,
 * typically when a player joins or leaves the clan.
 *
 * @property player The player associated with the member count change
 * @property clanName The name of the clan
 * @property newMemberCount The new total member count of the clan
 */
data class ClanMemberCountChangeEvent(
    val player: OfflinePlayer,
    val clanName: String,
    val newMemberCount: Int
) : SurfSyncEvent()
