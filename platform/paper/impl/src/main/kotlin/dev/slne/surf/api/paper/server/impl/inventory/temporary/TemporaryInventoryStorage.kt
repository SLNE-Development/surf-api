package dev.slne.surf.api.paper.server.impl.inventory.temporary

import dev.slne.surf.api.common.util.logger
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import java.io.IOException

/**
 * Reads and writes the [StoredEntry] in a player's persistent data container, the only place the
 * original inventory lives.
 *
 * Call it only on the thread that owns the player.
 */
internal object TemporaryInventoryStorage {

    private val log = logger()

    private val KEY = NamespacedKey("surf-api", "temporary_inventory")

    /**
     * The player's entry, an empty one when the player has none, or `null` when the stored bytes
     * are in an unknown format version or malformed. Such an entry is logged and must stay
     * untouched.
     */
    fun read(player: Player): StoredEntry? {
        val bytes = player.persistentDataContainer.get(KEY, PersistentDataType.BYTE_ARRAY)
            ?: return StoredEntry(emptyList(), emptyList())

        val decoded = try {
            TemporaryInventoryFormat.decode(bytes)
        } catch (failure: IOException) {
            log.atSevere()
                .withCause(failure)
                .log(
                    "The temporary inventory entry of %s is malformed; it stays untouched.",
                    player.name,
                )

            return null
        }

        return when (decoded) {
            is DecodedEntry.Supported -> decoded.entry
            is DecodedEntry.UnknownVersion -> {
                log.atWarning()
                    .log(
                        "The temporary inventory entry of %s has the unknown format version %d; " +
                            "it stays untouched.",
                        player.name,
                        decoded.version,
                    )

                null
            }
        }
    }

    /**
     * Stores [entry] for the player, or removes the player's entry when [entry] is empty.
     */
    fun write(player: Player, entry: StoredEntry) {
        val container = player.persistentDataContainer

        if (entry.isEmpty) {
            container.remove(KEY)
        } else {
            val bytes = TemporaryInventoryFormat.encode(entry)

            container.set(KEY, PersistentDataType.BYTE_ARRAY, bytes)
        }
    }
}
