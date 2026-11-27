package dev.slne.surf.api.paper.server.impl.inventory.temporary

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.paper.inventory.temporary.TemporaryInventoryEndCause
import dev.slne.surf.api.spring.SurfLifecycle
import org.bukkit.Server
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.plugin.Plugin
import org.springframework.stereotype.Component

/**
 * Ends the temporary inventory sessions of a plugin that is being disabled.
 *
 * While the server stops, every session of the plugin ends at once in its [PluginDisableEvent].
 * The event fires before the plugin's disable phase, so the owner still receives the end event. On
 * Paper it fires on the main thread; on SurfCanvas on the region shutdown thread, which passes every
 * ownership check although the region threads are already halted.
 *
 * While the server keeps running, each end is scheduled on the player's entity scheduler on behalf
 * of surf-api's [plugin] and runs after the owner's disable phase. A session that ended in between
 * stays ended, and a session begun since stays untouched.
 */
@Component
internal class TemporaryInventoryOwnerDisableListener(
    private val sessions: TemporaryInventoryService,
    private val server: Server,
    private val plugin: Plugin,
) : Listener, SurfLifecycle {

    private val log = logger()

    override suspend fun onEnable() {
        server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler
    fun onPluginDisable(event: PluginDisableEvent) {
        val owned = sessions.activeSessionsOwnedBy(event.plugin.name)
        val isShutdown = server.isStopping

        for (session in owned) {
            val player = server.getPlayer(session.playerUuid) ?: continue

            if (isShutdown) {
                endAtShutdown(player, session)
            } else {
                player.scheduler.run(plugin, { endOwnerDisabled(player, session) }, null)
            }
        }
    }

    private fun endAtShutdown(player: Player, session: ActiveTemporaryInventorySession) {
        try {
            sessions.end(player, TemporaryInventoryEndCause.SHUTDOWN)
        } catch (failure: Exception) {
            log.atSevere()
                .withCause(failure)
                .log("Could not end %s at shutdown.", session)
        }
    }

    private fun endOwnerDisabled(player: Player, session: ActiveTemporaryInventorySession) {
        if (!session.isActive) return

        sessions.end(player, TemporaryInventoryEndCause.OWNER_DISABLED)
    }
}
