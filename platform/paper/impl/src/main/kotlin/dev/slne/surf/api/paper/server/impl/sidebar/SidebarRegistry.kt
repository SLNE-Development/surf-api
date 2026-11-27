package dev.slne.surf.api.paper.server.impl.sidebar

import dev.slne.surf.api.paper.event.BukkitListener
import dev.slne.surf.api.spring.SurfLifecycle
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.server.PluginDisableEvent
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks the open sidebars, passes player joins and quits to them, and closes a plugin's sidebars
 * when the plugin is disabled and every remaining one when surf-api is disabled.
 */
@BukkitListener
class SidebarRegistry : Listener, SurfLifecycle {
    private val sidebars = ConcurrentHashMap.newKeySet<SurfSidebarImpl>()

    fun add(sidebar: SurfSidebarImpl) {
        sidebars.add(sidebar)
    }

    fun remove(sidebar: SurfSidebarImpl) {
        sidebars.remove(sidebar)
    }

    override suspend fun onDisable() {
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
