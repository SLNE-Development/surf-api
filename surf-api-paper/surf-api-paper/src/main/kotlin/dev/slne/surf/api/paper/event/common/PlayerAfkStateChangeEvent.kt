package dev.slne.surf.api.paper.event.common

import dev.slne.surf.api.core.event.SurfSyncEvent
import java.util.*

/**
 * Represents an event triggered when a player's AFK state changes.
 *
 * This event provides details about the player whose AFK state is changing,
 * including their unique identifier and the previous and new AFK states.
 * It is dispatched synchronously via the SurfEventBus to registered handlers,
 * allowing listeners to react to the state change.
 *
 * @param playerUuid The unique identifier of the player whose AFK state is changing.
 * @param fromState The player's previous AFK state (true if they were AFK, false otherwise).
 * @param toState The player's new AFK state (true if now AFK, false otherwise).
 */
data class PlayerAfkStateChangeEvent(
    val playerUuid: UUID,
    val fromState: Boolean,
    val toState: Boolean,
) : SurfSyncEvent()