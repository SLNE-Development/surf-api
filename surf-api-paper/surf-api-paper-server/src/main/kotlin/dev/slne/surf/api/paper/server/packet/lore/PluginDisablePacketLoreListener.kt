package dev.slne.surf.api.paper.server.packet.lore

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginDisableEvent

object PluginDisablePacketLoreListener : Listener {

    @EventHandler
    fun onPluginDisable(event: PluginDisableEvent) {
        PacketLoreListener.unregister(event.plugin)
    }
}