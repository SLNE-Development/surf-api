package dev.slne.surf.api.paper.inventory.temporary

import dev.slne.surf.api.event.SurfSyncEvent
import io.papermc.paper.persistence.PersistentDataContainerView
import net.kyori.adventure.key.Key
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*

/**
 * Fired once after a temporary inventory session ended and the original inventory is back.
 *
 * It fires on the thread that owns the player, for every [cause]. A plugin reverts the state it
 * changed for the session here, such as flight or a world border, and reads what it stored for
 * that in [state].
 *
 * @property key the kind of the session
 * @property state the persistent data the owning plugin stored for the session, read-only
 * @property discardedItems the temporary items the player carried in the inventory, on the cursor
 * and in the crafting grid; they no longer exist anywhere else
 */
class TemporaryInventoryEndEvent(
    val player: Player,
    val key: Key,
    val sessionId: UUID,
    val cause: TemporaryInventoryEndCause,
    val state: PersistentDataContainerView,
    val discardedItems: List<ItemStack>,
) : SurfSyncEvent() {

    override fun toString(): String {
        return "TemporaryInventoryEndEvent(player=${player.name}, key=${key.asString()}, " +
            "sessionId=$sessionId, cause=$cause, discardedItems=$discardedItems)"
    }
}

/**
 * Why a temporary inventory session ended.
 */
enum class TemporaryInventoryEndCause {

    /**
     * The owning plugin ended the session.
     */
    ENDED,

    /**
     * The player quit.
     */
    QUIT,

    /**
     * The player died. No temporary item dropped, and the player respawns with the original
     * inventory.
     */
    DEATH,

    /**
     * The server stopped while the owning plugin was still enabled.
     */
    SHUTDOWN,

    /**
     * The owning plugin was disabled while the server kept running. The owning plugin no longer
     * receives the event.
     */
    OWNER_DISABLED,

    /**
     * The player joined with a session that a crash left in the player's data. The owning plugin
     * may not be installed any more, and the session cannot be resumed.
     */
    RECOVERED,
}
