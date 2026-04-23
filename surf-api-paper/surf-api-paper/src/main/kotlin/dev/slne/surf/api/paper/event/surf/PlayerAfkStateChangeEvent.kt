package dev.slne.surf.api.paper.event.surf

import dev.slne.surf.api.core.event.SurfCancellableEvent
import java.util.UUID

/**
 * Fired through `SurfEventBus` when a player's AFK state changes.
 *
 * Cancelling the event prevents subsequent handlers (that did not opt out of
 * cancelled events) from running. The plugin firing this event is responsible
 * for honouring [isCancelled] when deciding whether to actually flip the
 * player's AFK flag.
 *
 * @property fromState the previous AFK state of the player.
 * @property toState   the new AFK state the player is being moved to.
 */
class PlayerAfkStateChangeEvent(
    playerUuid: UUID,
    val fromState: Boolean,
    val toState: Boolean,
) : AbstractSurfPlayerEvent(playerUuid), SurfCancellableEvent {

    override var isCancelled: Boolean = false
}
