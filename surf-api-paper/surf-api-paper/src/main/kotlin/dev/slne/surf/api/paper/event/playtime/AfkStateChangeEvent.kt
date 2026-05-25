package dev.slne.surf.api.paper.event.playtime

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

/**
 * Fired when a player's AFK (Away From Keyboard) state changes.
 *
 * This synchronous event is triggered when a player transitions between AFK and active states.
 *
 * @property player The player whose AFK state changed
 * @property fromState The previous AFK state (true = AFK, false = active)
 * @property toState The new AFK state (true = AFK, false = active)
 */
data class AfkStateChangeEvent(
    val player: Player,
    val fromState: Boolean,
    val toState: Boolean,
) : SurfSyncEvent()