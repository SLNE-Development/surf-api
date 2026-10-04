package dev.slne.surf.api.minestom.visibility

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import java.util.function.Predicate

/**
 * Restricts what players see.
 *
 * When the server limits how many players a player sees at once, it owns the viewer rule of every
 * player; restrict what a player sees through [updateViewerRule] instead of
 * `Player#updateViewerRule`, which would remove the limit for that player. Without such a limit,
 * these functions simply update the player's viewer rule.
 */
object PlayerVisibility {

    @Volatile
    private var provider: Provider? = null

    /**
     * Restricts the entities [player] sees to those [predicate] accepts, on top of the limit of
     * nearest players. `null` lifts the restriction.
     */
    fun updateViewerRule(player: Player, predicate: Predicate<in Entity>?) {
        if (provider?.updateViewerRule(player, predicate) != true) {
            player.updateViewerRule(predicate)
        }
    }

    /**
     * Re-evaluates the viewer rule of [player], like `Player#updateViewerRule()`.
     */
    fun updateViewerRule(player: Player) {
        if (provider?.refreshViewerRule(player) != true) {
            player.updateViewerRule()
        }
    }

    /** Whether the limit currently restricts the players [player] sees. */
    fun isLimited(player: Player): Boolean = provider?.isLimited(player) == true

    /** Implemented by the server when it limits the visible players. */
    @InternalSurfApi
    interface Provider {
        /** @return whether the rule was taken over, `false` to let the player apply it itself */
        fun updateViewerRule(player: Player, predicate: Predicate<in Entity>?): Boolean

        /** @return whether the refresh was taken over */
        fun refreshViewerRule(player: Player): Boolean

        fun isLimited(player: Player): Boolean
    }

    @InternalSurfApi
    fun installProvider(provider: Provider?) {
        this.provider = provider
    }
}
