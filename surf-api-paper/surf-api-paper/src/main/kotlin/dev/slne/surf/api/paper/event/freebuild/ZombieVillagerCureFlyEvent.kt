package dev.slne.surf.api.paper.event.freebuild

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

/**
 * Fired when a player cures a zombie villager and is able to fly as a reward.
 *
 * This synchronous event is triggered when a player successfully cures a zombie villager,
 * granting them flight capabilities as a reward.
 *
 * @property player The player who cured the zombie villager
 */
data class ZombieVillagerCureFlyEvent(
    val player: Player
) : SurfSyncEvent()
