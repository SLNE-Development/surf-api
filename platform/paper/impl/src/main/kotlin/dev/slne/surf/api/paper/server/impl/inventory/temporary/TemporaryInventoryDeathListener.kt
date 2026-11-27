package dev.slne.surf.api.paper.server.impl.inventory.temporary

import dev.slne.surf.api.paper.event.register
import dev.slne.surf.api.paper.inventory.temporary.TemporaryInventoryEndCause
import dev.slne.surf.api.spring.SurfLifecycle
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.springframework.stereotype.Component

/**
 * Ends the temporary inventory session of a player who dies, with the cause
 * [TemporaryInventoryEndCause.DEATH].
 *
 * The server computes the drops from the temporary inventory before [PlayerDeathEvent] and wipes
 * the inventory after it. The death handler clears every drop, keeps the inventory and puts the
 * original inventory back inside the event. Cancelled deaths change nothing.
 *
 * The view the dead player had open closes after the event, when the end has already discarded
 * its cursor item and crafting inputs. The close handler covers a session the death handler could
 * not end, before the server drops those items.
 */
@Component
class TemporaryInventoryDeathListener(
    private val service: TemporaryInventoryService,
) : Listener, SurfLifecycle {

    override suspend fun onEnable() {
        register()
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onDeath(event: PlayerDeathEvent) {
        val player = event.player
        if (service.activeSession(player.uniqueId) == null) return

        event.drops.clear()
        event.keepInventory = true
        service.end(player, TemporaryInventoryEndCause.DEATH)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onDeathClose(event: InventoryCloseEvent) {
        if (event.reason != InventoryCloseEvent.Reason.DEATH) return

        val player = event.player as? Player ?: return
        if (service.activeSession(player.uniqueId) == null) return

        service.discardLooseItems(player)
    }
}
