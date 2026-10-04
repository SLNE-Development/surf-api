package dev.slne.surf.api.minestom.player.event

import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.event.trait.AsyncEvent
import net.minestom.server.event.trait.PlayerEvent
import net.minestom.server.instance.Instance
import org.jetbrains.annotations.ApiStatus

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
class AsyncPlayerSpawnLocationEvent @ApiStatus.Internal constructor(
    private val player: Player,

    /** The instance the player spawns in. */
    var instance: Instance,

    /** The position the player spawns at. */
    var position: Pos,
) : PlayerEvent, AsyncEvent {

    override fun getPlayer() = player
}
