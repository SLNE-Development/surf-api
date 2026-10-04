package dev.slne.surf.api.minestom.player.event

import net.minestom.server.entity.Player
import net.minestom.server.event.trait.CancellableEvent
import net.minestom.server.event.trait.PlayerInstanceEvent
import org.jetbrains.annotations.ApiStatus

/**
 * Called when a player who is allowed to fly tries to start or stop flying.
 *
 * Cancelling the event keeps the player in the state they were in.
 */
class PlayerToggleFlightEvent @ApiStatus.Internal constructor(
    private val player: Player,

    /**
     * Whether the player is trying to start or stop flying.
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
