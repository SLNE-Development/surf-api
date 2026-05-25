package dev.slne.surf.api.paper.event.clan

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.OfflinePlayer

/**
 * Fired when a player is demoted within their clan.
 *
 * This synchronous event is triggered when a player receives a demotion to a lower rank
 * or role within their clan.
 *
 * @property player The player who was demoted
 * @property clanName The name of the clan
 * @property newRole The new role the player was demoted to
 */
data class ClanDemotedEvent(
    val player: OfflinePlayer,
    val clanName: String,
    val newRole: String
) : SurfSyncEvent()
