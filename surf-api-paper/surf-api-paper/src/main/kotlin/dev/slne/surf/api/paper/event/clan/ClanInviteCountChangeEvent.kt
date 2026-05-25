package dev.slne.surf.api.paper.event.clan

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.OfflinePlayer

/**
 * Fired when the invite count of a clan changes.
 *
 * This synchronous event is triggered when the number of pending invitations in a clan changes,
 * typically when a player sends or receives a clan invitation.
 *
 * @property player The player associated with the invite count change
 * @property clanName The name of the clan
 * @property newInviteCount The new total invite count of the clan
 */
data class ClanInviteCountChangeEvent(
    val player: OfflinePlayer,
    val clanName: String,
    val newInviteCount: Int
) : SurfSyncEvent()
