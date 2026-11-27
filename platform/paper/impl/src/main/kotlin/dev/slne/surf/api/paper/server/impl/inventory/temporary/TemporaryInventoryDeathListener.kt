package dev.slne.surf.api.paper.server.impl.inventory.temporary

import dev.slne.surf.api.paper.event.BukkitListener
import dev.slne.surf.api.paper.inventory.temporary.TemporaryInventoryEndCause
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryCloseEvent

/**
 * Ends the temporary inventory session of a player who dies, with the cause
 * [TemporaryInventoryEndCause.DEATH].
 *
 * The server computes the drops from the temporary inventory before [PlayerDeathEvent] and wipes
 * the inventory after it, unless the event keeps it. At [EventPriority.HIGHEST] the death handler
 * clears every drop and keeps the inventory, so later listeners see no temporary drop. At
 * [EventPriority.MONITOR] it does both again, in case a later listener changed them, and puts the
 * original inventory back. A death that any listener cancels changes nothing.
 *
 * The view the dead player had open closes after the event, when the end has already discarded
 * its loose items. The close handler covers a session the death handler could not end, before the
 * server drops those items.
 */
@BukkitListener
internal class TemporaryInventoryDeathListener(
    private val service: TemporaryInventoryService,
) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onDeath(event: PlayerDeathEvent) {
        if (service.activeSession(event.player.uniqueId) == null) return

        event.keepTemporaryItemsOut()
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onDeathOutcome(event: PlayerDeathEvent) {
        val player = event.player
        if (service.activeSession(player.uniqueId) == null) return

        event.keepTemporaryItemsOut()
        service.end(player, TemporaryInventoryEndCause.DEATH)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onDeathClose(event: InventoryCloseEvent) {
        if (event.reason != InventoryCloseEvent.Reason.DEATH) return

        val player = event.player as? Player ?: return
        if (service.activeSession(player.uniqueId) == null) return

        service.discardLooseItems(player)
    }

    private fun PlayerDeathEvent.keepTemporaryItemsOut() {
        drops.clear()
        keepInventory = true
    }
}
