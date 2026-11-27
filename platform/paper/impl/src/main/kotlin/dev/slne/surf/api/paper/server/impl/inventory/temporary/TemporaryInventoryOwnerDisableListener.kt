package dev.slne.surf.api.paper.server.impl.inventory.temporary

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.paper.event.BukkitListener
import dev.slne.surf.api.paper.inventory.temporary.TemporaryInventoryEndCause
import org.bukkit.Server
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.event.server.PluginEnableEvent
import org.bukkit.plugin.Plugin

/**
 * Ends the temporary inventory sessions of a plugin that is being disabled.
 *
 * From its [PluginDisableEvent] on, the plugin cannot begin new sessions until it is enabled
 * again.
 *
 * While the server stops, every session of the plugin ends at once in its [PluginDisableEvent].
 * The event fires before the plugin's disable phase, so the owner still receives the end event. On
 * Paper it fires on the main thread; on SurfCanvas on the region shutdown thread, which passes
 * every ownership check although the region threads are already halted.
 *
 * While the server keeps running, each end is scheduled on the player's entity scheduler on behalf
 * of surf-api's [plugin]. On Paper it runs after the owner's disable phase. On Folia and
 * SurfCanvas it runs in the player's next region tick, which can fall within the owner's disable
 * phase. A session that ended in between stays ended.
 */
@BukkitListener
internal class TemporaryInventoryOwnerDisableListener(
    private val sessions: TemporaryInventoryService,
    private val server: Server,
    private val plugin: Plugin,
) : Listener {

    private val log = logger()

    @EventHandler
    fun onPluginEnable(event: PluginEnableEvent) {
        sessions.openAdmission(event.plugin.name)
    }

    @EventHandler
    fun onPluginDisable(event: PluginDisableEvent) {
        val owned = sessions.closeAdmission(event.plugin.name)
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
