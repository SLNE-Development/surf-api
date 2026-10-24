package dev.slne.surf.api.minestom.player.event

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import net.minestom.server.entity.Player
import net.minestom.server.event.trait.CancellableEvent
import net.minestom.server.event.trait.PlayerInstanceEvent

/**
 * Called when a player who is allowed to fly tries to start or stop flying.
 *
 * Cancelling the event keeps the player in the state they were in.
 */
class PlayerToggleFlightEvent @InternalSurfApi constructor(
    private val player: Player,

    /**
     * The requested state: `true` to start flying, `false` to stop.
     */
    val isFlying: Boolean,
) : PlayerInstanceEvent, CancellableEvent {
    private var cancelled = false

    override fun isCancelled(): Boolean = cancelled
    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    override fun getPlayer() = player
}
