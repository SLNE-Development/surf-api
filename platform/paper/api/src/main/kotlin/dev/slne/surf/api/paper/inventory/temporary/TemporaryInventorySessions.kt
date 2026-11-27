package dev.slne.surf.api.paper.inventory.temporary

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.api.spring.surfBean
import net.kyori.adventure.key.Key
import org.bukkit.entity.Player
import org.bukkit.inventory.PlayerInventory
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.plugin.Plugin
import java.util.*

/**
 * Backs the temporary inventory session extensions on `Player`.
 */
@InternalSurfApi
interface TemporaryInventorySessions {

    suspend fun begin(
        player: Player,
        plugin: Plugin,
        key: Key,
        fill: (inventory: PlayerInventory, state: PersistentDataContainer) -> Unit,
    ): TemporaryInventoryBeginResult

    fun activeSession(playerUuid: UUID): TemporaryInventorySession?

    @InternalSurfApi
    companion object {
        internal val instance by surfBean<TemporaryInventorySessions>()
    }
}
