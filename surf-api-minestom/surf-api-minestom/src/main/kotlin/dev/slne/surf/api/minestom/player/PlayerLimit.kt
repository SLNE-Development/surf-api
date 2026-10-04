package dev.slne.surf.api.minestom.player

import dev.slne.surf.api.minestom.extension.ConnectionManager
import dev.slne.surf.api.minestom.player.event.AsyncPlayerCountEvent

/**
 * How many players this server admits.
 *
 * A player who would exceed [maxPlayers] is refused while joining, which a listener of
 * [AsyncPlayerCountEvent] can override. The limit is also reported on the server list.
 */
object PlayerLimit {

    @Volatile
    private var limit: Int? = null

    /**
     * The number of players the server admits, or `null` when it admits any number.
     *
     * @throws IllegalArgumentException if the new value is not greater than zero
     */
    var maxPlayers: Int?
        get() = limit
        set(value) {
            require(value == null || value > 0) { "maxPlayers must be greater than zero, was $value" }
            limit = value
        }

    /** The number of connected players, counting those still in the configuration phase. */
    val playerCount: Int
        get() = ConnectionManager.onlinePlayerCount + ConnectionManager.configPlayers.size

    /** Whether [playerCount] has reached [maxPlayers]. */
    val isFull: Boolean
        get() = limit?.let { playerCount >= it } ?: false
}
