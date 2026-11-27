package dev.slne.surf.api.paper.server.impl.inventory.temporary

import dev.slne.surf.api.paper.inventory.temporary.TemporaryInventorySession
import net.kyori.adventure.key.Key
import org.bukkit.plugin.Plugin
import java.util.*

/**
 * A session that began in this server run; a handle on the session stored in the player's data.
 *
 * @property owner the plugin that began the session
 */
class ActiveTemporaryInventorySession internal constructor(
    override val id: UUID,
    override val key: Key,
    override val playerUuid: UUID,
    val owner: Plugin,
    private val service: TemporaryInventoryService,
) : TemporaryInventorySession {

    override val isActive: Boolean
        get() = service.isActive(this)

    override fun end(): Boolean = service.end(this)

    override suspend fun endAsync(): Boolean = service.endAsync(this)

    override fun toString(): String {
        return "TemporaryInventorySession(id=$id, key=${key.asString()}, playerUuid=$playerUuid, " +
            "owner=${owner.name})"
    }
}
