package dev.slne.surf.api.minestom.player.event

import dev.slne.surf.api.minestom.player.PlayerLimit
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import net.minestom.server.event.trait.AsyncEvent
import net.minestom.server.event.trait.PlayerEvent
import org.jetbrains.annotations.ApiStatus

/**
 * Called once per player, before the server configures them, to decide whether they may join.
 *
 * [result] arrives as [Result.KICK_FULL] when the player would exceed [PlayerLimit.maxPlayers] and
 * as [Result.ALLOWED] otherwise. A listener may [allow] a refused player in anyway, e.g. one with
 * a bypass permission, or [disallow] one the server would have taken. A refused player is kicked
 * with [kickMessage] before the configuration continues.
 *
 * ```
 * node.addListener<AsyncPlayerCountEvent> { event ->
 *     if (event.result == AsyncPlayerCountEvent.Result.KICK_FULL &&
 *         event.player.hasPermission("server.bypass-max-players")
 *     ) {
 *         event.allow()
 *     }
 * }
 * ```
 *
 * Listeners run on the player's own virtual configuration thread, so they may block.
 */
class AsyncPlayerCountEvent @ApiStatus.Internal constructor(
    private val player: Player,

    /** The number of connected players, counting this one and those still being configured. */
    val playerCount: Int,

    /** The number of players the server admits, or `null` when it admits any number. */
    val maxPlayers: Int?,

    /** Whether the player may join, and why not if they may not. */
    var result: Result,

    /** The message a refused player is disconnected with. */
    var kickMessage: Component,
) : PlayerEvent, AsyncEvent {

    val isAllowed: Boolean get() = result == Result.ALLOWED

    /** Lets the player join, whatever an earlier listener or the server itself decided. */
    fun allow() {
        result = Result.ALLOWED
    }

    /** Refuses the player with [result] and disconnects them with [kickMessage]. */
    fun disallow(result: Result, kickMessage: Component) {
        require(result != Result.ALLOWED) { "disallow needs a refusing result, was $result" }

        this.result = result
        this.kickMessage = kickMessage
    }

    override fun getPlayer() = player

    enum class Result {
        ALLOWED,

        /** The server has no free slot left. */
        KICK_FULL,

        /** A listener refused the player for its own reason. */
        KICK_OTHER,
    }
}
