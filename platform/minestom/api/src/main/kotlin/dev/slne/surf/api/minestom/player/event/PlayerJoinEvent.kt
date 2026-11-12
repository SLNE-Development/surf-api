package dev.slne.surf.api.minestom.player.event

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import net.minestom.server.event.trait.PlayerInstanceEvent

/**
 * Called once a player joined the server and spawned for the first time.
 *
 * [joinMessage] is broadcast to every player and the console afterwards; set it to `null` to
 * join silently.
 */
class PlayerJoinEvent @InternalSurfApi constructor(
    private val player: Player,

    /**
     * The message announcing the player, or `null` for none.
     */
    var joinMessage: Component?,
) : PlayerInstanceEvent {

    override fun getPlayer() = player
}
