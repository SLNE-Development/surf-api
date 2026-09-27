package dev.slne.surf.api.paper.server.impl.sidebar

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.server.PluginDisableEvent
import java.util.concurrent.ConcurrentHashMap

object SidebarRegistry : Listener {
    private val sidebars = ConcurrentHashMap.newKeySet<SurfSidebarImpl>()

    fun add(sidebar: SurfSidebarImpl) {
        sidebars.add(sidebar)
    }

    fun remove(sidebar: SurfSidebarImpl) {
        sidebars.remove(sidebar)
    }

    fun closeAll() {
        sidebars.toList().forEach { it.close() }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        sidebars.forEach { it.onPlayerJoin(event.player) }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        sidebars.forEach { it.onPlayerQuit(event.player) }
    }

    @EventHandler
    fun onPluginDisable(event: PluginDisableEvent) {
        sidebars.filter { it.plugin === event.plugin }.forEach { it.close() }
    }
}
