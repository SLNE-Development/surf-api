package dev.slne.surf.api.paper.server.impl.inventory.temporary

import dev.slne.surf.api.paper.event.register
import dev.slne.surf.api.paper.inventory.temporary.TemporaryInventoryEndCause
import dev.slne.surf.api.spring.SurfLifecycle
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Ends temporary inventory sessions with [TemporaryInventoryEndCause.QUIT] when the player quits,
 * before the server saves the player for the last time.
 *
 * A view still open when the server removes the player closes with
 * [InventoryCloseEvent.Reason.DISCONNECT], after which the server drops the cursor item and the
 * inputs of a workstation menu, such as a crafting table or an anvil, into the world. Those
 * temporary items are discarded on that close and reported with the end event. The cursor item and
 * the player's own crafting grid are discarded at the quit.
 */
@Component
class TemporaryInventoryQuitListener(
    private val sessions: TemporaryInventoryService,
) : Listener, SurfLifecycle {

    private val discardedOnDisconnect = ConcurrentHashMap<UUID, List<ItemStack>>()

    override suspend fun onEnable() {
        register()
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onDisconnectClose(event: InventoryCloseEvent) {
        if (event.reason != InventoryCloseEvent.Reason.DISCONNECT) return

        val player = event.player as? Player ?: return
        if (sessions.activeSession(player.uniqueId) == null) return

        discardedOnDisconnect.merge(
            player.uniqueId,
            sessions.discardLooseItems(player),
            List<ItemStack>::plus,
        )
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        val discarded = discardedOnDisconnect.remove(player.uniqueId).orEmpty()

        sessions.end(player, TemporaryInventoryEndCause.QUIT, discarded)
    }
}
