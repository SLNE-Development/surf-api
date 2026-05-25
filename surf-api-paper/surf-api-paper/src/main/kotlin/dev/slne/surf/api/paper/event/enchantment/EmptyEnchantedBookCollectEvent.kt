package dev.slne.surf.api.paper.event.enchantment

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

/**
 * Fired when a player collects an empty enchanted book.
 *
 * This synchronous event is triggered when a player picks up or collects an enchanted book
 * that is empty or has no enchantments applied to it.
 *
 * @property player The player who collected the empty enchanted book
 */
data class EmptyEnchantedBookCollectEvent(
    val player: Player
) : SurfSyncEvent()
