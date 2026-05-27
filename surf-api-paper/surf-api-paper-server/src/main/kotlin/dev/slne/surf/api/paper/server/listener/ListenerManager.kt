package dev.slne.surf.api.paper.server.listener

import dev.slne.surf.api.paper.event.register
import dev.slne.surf.api.paper.server.command.SuspendRequirementServiceImpl
import dev.slne.surf.api.paper.server.impl.glow.GlowingListener
import dev.slne.surf.api.paper.server.impl.pdc.block.BlockDataListener
import dev.slne.surf.api.paper.server.impl.visualizer.visualizer.VisualizerListener
import dev.slne.surf.api.paper.server.plugin
import org.bukkit.Bukkit

object ListenerManager {
    /**
     * Registers all listeners.
     */
    fun registerListeners() {
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord")
        VisualizerListener.register()
        GlowingListener.register()

        BlockDataListener.register()

        SuspendRequirementServiceImpl.get().getEventListener().register()
    }

    /**
     * Unregisters all listeners.
     */
    fun unregisterListeners() {
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, "BungeeCord")
    }
}
