package dev.slne.surf.api.minestom.player.event

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.event.trait.AsyncEvent
import net.minestom.server.event.trait.PlayerEvent
import net.minestom.server.instance.Instance

/**
 * Called once per player, after [net.minestom.server.event.player.AsyncPlayerConfigurationEvent],
 * to decide where the player spawns when they join.
 *
 * Starts out with the spawning instance and the respawn point the configuration event chose.
 * [position] also becomes the player's respawn point.
 *
 * Listeners run on the player's own virtual configuration thread, so they may block, e.g. to load
 * the position the player logged out at.
 */
class AsyncPlayerSpawnLocationEvent @InternalSurfApi constructor(
    private val player: Player,

    /**
     * The instance the player spawns in.
     */
    var instance: Instance,

    /**
     * The position the player spawns at.
     */
    var position: Pos,
) : PlayerEvent, AsyncEvent {

    override fun getPlayer() = player
}
