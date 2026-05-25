package dev.slne.surf.api.paper.event.clan

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.OfflinePlayer

/**
 * Fired when a player is promoted within their clan.
 *
 * This synchronous event is triggered when a player receives a promotion to a higher rank
 * or role within their clan.
 *
 * @property player The player who was promoted
 * @property clanName The name of the clan
 * @property newRole The new role the player was promoted to
 */
data class ClanPromotedEvent(
    val player: OfflinePlayer,
    val clanName: String,
    val newRole: String
) : SurfSyncEvent()
