package dev.slne.surf.api.paper.event.surf

import dev.slne.surf.api.core.event.SurfCancellableEvent
import java.util.UUID

/**
 * Fired when a player's AFK state transitions.
 *
 * @param playerUuid UUID of the affected player.
 * @param fromState the previous AFK state (`true` = was AFK).
 * @param toState the new AFK state (`true` = is now AFK).
 */
class PlayerAfkStateChangeEvent(
    playerUuid: UUID,
    val fromState: Boolean,
    val toState: Boolean,
) : AbstractSurfPlayerEvent(playerUuid), SurfCancellableEvent {
    override var isCancelled: Boolean = false
}
