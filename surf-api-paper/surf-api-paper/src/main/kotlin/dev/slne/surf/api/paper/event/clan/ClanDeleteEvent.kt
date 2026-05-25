package dev.slne.surf.api.paper.event.clan

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

/**
 * Fired when a clan is deleted.
 *
 * This synchronous event is triggered when a clan is dissolved or deleted from the server.
 *
 * @property player The player who deleted the clan
 * @property clanName The name of the deleted clan
 * @property clanTag The tag of the deleted clan
 */
data class ClanDeleteEvent(
    val player: Player,
    val clanName: String,
    val clanTag: String
) : SurfSyncEvent()
