package dev.slne.surf.api.paper.server.listener

import dev.slne.surf.api.paper.event.register
import dev.slne.surf.api.paper.server.command.SuspendRequirementServiceImpl
import dev.slne.surf.api.paper.server.plugin
import org.bukkit.Bukkit

object ListenerManager {
    /**
     * Registers the listeners that are not beans; listener beans are registered with the root
     * context.
     */
    fun registerListeners() {
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord")
        SuspendRequirementServiceImpl.instance.getEventListener().register()
    }

    /**
     * Unregisters all listeners.
     */
    fun unregisterListeners() {
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, "BungeeCord")
    }
}
