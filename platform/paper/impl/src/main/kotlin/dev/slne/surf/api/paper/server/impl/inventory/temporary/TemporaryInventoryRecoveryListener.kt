package dev.slne.surf.api.paper.server.impl.inventory.temporary

import dev.slne.surf.api.paper.event.register
import dev.slne.surf.api.paper.inventory.temporary.TemporaryInventoryEndCause
import dev.slne.surf.api.spring.SurfLifecycle
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.springframework.stereotype.Component

/**
 * Ends, with [TemporaryInventoryEndCause.RECOVERED], a session that a joining player's data still
 * holds although no session is active for the player in this server run.
 *
 * Such a session survived a crash. The player's data was saved together with the session, so the
 * original inventory put back is exactly the one that belongs to the loaded inventory. Recovery
 * needs neither the owning plugin nor its classes; a recovered session cannot be resumed.
 *
 * It runs at [EventPriority.LOWEST], so every other join listener already sees the original
 * inventory.
 */
@Component
internal class TemporaryInventoryRecoveryListener(
    private val sessions: TemporaryInventoryService,
) : Listener, SurfLifecycle {

    override suspend fun onEnable() {
        register()
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (sessions.activeSession(player.uniqueId) != null) return

        sessions.end(player, TemporaryInventoryEndCause.RECOVERED)
    }
}
