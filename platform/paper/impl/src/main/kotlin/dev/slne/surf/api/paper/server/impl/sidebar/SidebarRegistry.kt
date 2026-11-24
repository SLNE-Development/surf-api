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
        for (sidebar in sidebars.toList()) {
            sidebar.close()
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        for (sidebar in sidebars) {
            sidebar.onPlayerJoin(event.player)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        for (sidebar in sidebars) {
            sidebar.onPlayerQuit(event.player)
        }
    }

    @EventHandler
    fun onPluginDisable(event: PluginDisableEvent) {
        val pluginSidebars = sidebars.filter { it.plugin === event.plugin }
        for (sidebar in pluginSidebars) {
            sidebar.close()
        }
    }
}
