package dev.slne.surf.api.paper.event.settings

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

/**
 * Fired when a player changes multiple settings in a batch.
 *
 * This synchronous event is triggered when a player makes changes to multiple settings
 * at once, such as through a settings GUI or batch configuration.
 *
 * @property player The player who changed their settings
 * @property amount The number of settings that were changed in this batch
 */
data class SettingsBatchChangeEvent(
    val player: Player,
    val amount: Int
) : SurfSyncEvent()
