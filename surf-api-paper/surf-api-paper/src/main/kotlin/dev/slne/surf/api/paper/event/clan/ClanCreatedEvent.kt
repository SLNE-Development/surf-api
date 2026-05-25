package dev.slne.surf.api.paper.event.clan

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

/**
 * Fired when a clan is created.
 *
 * This synchronous event is triggered when a player successfully creates a new clan.
 *
 * @property player The player who created the clan
 * @property clanName The name of the newly created clan
 * @property clanTag The tag of the newly created clan
 */
data class ClanCreatedEvent(
    val player: Player,
    val clanName: String,
    val clanTag: String
) : SurfSyncEvent()
