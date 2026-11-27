package dev.slne.surf.api.paper.scenario.probe

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.spring.SurfLifecycle
import org.bukkit.Server
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.plugin.Plugin
import org.springframework.stereotype.Component

/**
 * Logs what players do as [ProbeProtocol] event lines, so scenario tests observe the server's side
 * of each bot action.
 *
 * Every handler runs at [EventPriority.MONITOR] and changes nothing.
 *
 * SurfCanvas respawns a dead player without [PlayerRespawnEvent]. There a respawn is logged from
 * SurfCanvas's own respawn event, which fires once the player is back in the world.
 */
@Component
class ProbeEventLog(
    private val server: Server,
    private val plugin: Plugin,
) : Listener, SurfLifecycle {

    private val log = logger()

    override suspend fun onEnable() {
        val canvasRespawnEvent = try {
            Class.forName(CANVAS_POST_RESPAWN_EVENT).asSubclass(PlayerEvent::class.java)
        } catch (_: ClassNotFoundException) {
            return
        }

        server.pluginManager.registerEvent(
            canvasRespawnEvent,
            this,
            EventPriority.MONITOR,
            { _, event -> logEvent(ProbeProtocol.RESPAWN, (event as PlayerEvent).player) },
            plugin,
        )
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onJoin(event: PlayerJoinEvent) {
        logEvent(ProbeProtocol.JOIN, event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onQuit(event: PlayerQuitEvent) {
        logEvent(ProbeProtocol.QUIT, event.player, event.reason.name)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onDeath(event: PlayerDeathEvent) {
        logEvent(ProbeProtocol.DEATH, event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onRespawn(event: PlayerRespawnEvent) {
        logEvent(ProbeProtocol.RESPAWN, event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onOpen(event: InventoryOpenEvent) {
        val player = event.player as? Player ?: return

        logEvent(ProbeProtocol.OPEN, player, event.inventory.type.name)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return

        logEvent(ProbeProtocol.CLOSE, player, "${event.inventory.type.name} ${event.reason.name}")
    }

    private fun logEvent(kind: String, player: Player, detail: String = "") {
        log.atInfo()
            .log("%s %s %s %s", ProbeProtocol.EVENT, kind, player.name, detail)
    }

    private companion object {
        const val CANVAS_POST_RESPAWN_EVENT = "io.canvasmc.canvas.event.PlayerPostRespawnAsyncEvent"
    }
}
