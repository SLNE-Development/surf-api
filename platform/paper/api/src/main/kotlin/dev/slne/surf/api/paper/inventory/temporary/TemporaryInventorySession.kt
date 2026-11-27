package dev.slne.surf.api.paper.inventory.temporary

import net.kyori.adventure.key.Key
import java.util.*

/**
 * A temporary inventory session: the period in which a player carries a temporary inventory
 * instead of their original inventory.
 *
 * A player has at most one session. `Player.beginTemporaryInventorySession` begins it, and
 * `Player.temporaryInventorySession` returns the active one. The original inventory is stored in
 * the player's persistent data container, so it is saved together with the temporary inventory.
 *
 * Ending the session puts the original inventory back and fires one [TemporaryInventoryEndEvent].
 * Ending an ended session does nothing, so several code paths may end the same session.
 */
interface TemporaryInventorySession {

    val id: UUID

    /**
     * The kind of session, as given when it began.
     */
    val key: Key

    val playerUuid: UUID

    /**
     * Whether the session has not ended yet; readable from any thread.
     */
    val isActive: Boolean

    /**
     * Ends the session and puts the original inventory back.
     *
     * Every slot whose item the server can read again is restored, together with the selected
     * hotbar slot. An item it cannot read, for example one from a removed datapack, stays in the
     * player's data and is logged. The temporary items in the inventory, on the cursor and in the
     * crafting grid are discarded and reach listeners through [TemporaryInventoryEndEvent]. The
     * event fires before this function returns.
     *
     * @return `false` if the session had already ended
     * @throws IllegalStateException off the thread that owns the player
     */
    fun end(): Boolean

    /**
     * Runs [end] on the thread that owns the player, switching there first.
     *
     * @return `false` if the session had already ended, or the player left before the end ran
     */
    suspend fun endAsync(): Boolean
}
