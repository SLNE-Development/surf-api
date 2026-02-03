package dev.slne.surf.surfapi.bukkit.server.listener

import dev.slne.surf.surfapi.bukkit.api.event.register
import dev.slne.surf.surfapi.bukkit.server.gui.view.GuiViewListener
import dev.slne.surf.surfapi.bukkit.server.impl.glow.GlowingListener
import dev.slne.surf.surfapi.bukkit.server.impl.visualizer.visualizer.VisualizerListener
import dev.slne.surf.surfapi.bukkit.server.plugin
import org.bukkit.Bukkit

object ListenerManager {
    /**
     * Registers all listeners.
     */
    fun registerListeners() {
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord")
        VisualizerListener.register()
        GlowingListener.register()
        GuiViewListener.register()
    }

    /**
     * Unregisters all listeners.
     */
    fun unregisterListeners() {
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, "BungeeCord")
    }
}
