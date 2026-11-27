package dev.slne.surf.api.paper.inventory.temporary

import net.kyori.adventure.key.Key

/**
 * The outcome of `Player.beginTemporaryInventorySession`.
 */
sealed interface TemporaryInventoryBeginResult {

    /**
     * The session began; the player carries the temporary inventory.
     */
    class Started(val session: TemporaryInventorySession) : TemporaryInventoryBeginResult {

        override fun toString(): String = "Started(session=${session.id})"
    }

    /**
     * The player already has a session, so nothing changed.
     *
     * @property key the kind of the session the player already has
     */
    class AlreadyActive(val key: Key) : TemporaryInventoryBeginResult {

        override fun toString(): String = "AlreadyActive(key=${key.asString()})"
    }

    /**
     * An item of the original inventory could not be stored, so nothing changed.
     *
     * @property slot the player inventory slot of the item, in Bukkit's slot order
     * @property cause why the item could not be serialized
     */
    class Unstorable(val slot: Int, val cause: Exception) : TemporaryInventoryBeginResult {

        override fun toString(): String = "Unstorable(slot=$slot, cause=$cause)"
    }

    /**
     * No session can begin now, so nothing changed.
     *
     * The player is dead, offline or no longer in the world; the cursor item and the inputs of the
     * open workstation menu do not fit into the inventory; the owning plugin is disabled or being
     * disabled; or the player's data holds an entry this server cannot read.
     */
    data object Unavailable : TemporaryInventoryBeginResult
}
