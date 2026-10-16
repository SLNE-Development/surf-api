package dev.slne.surf.api.minestom.player.event

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import net.minestom.server.event.trait.PlayerEvent

/**
 * Called when a player who joined the server leaves it.
 *
 * [quitMessage] is broadcast to the remaining players and the console afterwards; set it to
 * `null` to leave silently.
 */
class PlayerQuitEvent @InternalSurfApi constructor(
    private val player: Player,

    /**
     * The message announcing the player left, or `null` for none.
     */
    var quitMessage: Component?,
) : PlayerEvent {

    override fun getPlayer() = player
}
