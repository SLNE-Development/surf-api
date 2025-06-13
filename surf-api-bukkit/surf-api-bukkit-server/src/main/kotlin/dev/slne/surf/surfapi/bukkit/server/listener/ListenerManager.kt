package dev.slne.surf.surfapi.bukkit.server.listener

import dev.slne.surf.surfapi.bukkit.api.event.register
import dev.slne.surf.surfapi.bukkit.api.inventory.listener.GuiListener
import dev.slne.surf.surfapi.bukkit.server.plugin
import org.bukkit.Bukkit

object ListenerManager {
    /**
     * Registers all listeners.
     */
    fun registerListeners() {
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord")

        GuiListener(plugin).register()
    }

    /**
     * Unregisters all listeners.
     */
    fun unregisterListeners() {
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, "BungeeCord")
    }
}
