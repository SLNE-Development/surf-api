package dev.slne.surf.api.paper.event.stats

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

/**
 * Fired when a player changes their statistics opt-out setting.
 *
 * This synchronous event is triggered when a player enables or disables the statistics tracking option.
 *
 * @property player The player who changed their stats opt-out setting
 */
data class StatsOptOutSettingEvent(
    val player: Player
) : SurfSyncEvent()
