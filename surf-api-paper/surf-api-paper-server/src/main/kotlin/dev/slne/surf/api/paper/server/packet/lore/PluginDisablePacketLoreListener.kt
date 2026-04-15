package dev.slne.surf.api.paper.server.packet.lore

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.common.NmsProvider
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginDisableEvent

@NmsUseWithCaution
object PluginDisablePacketLoreListener : Listener {
    private val packetLoreRegistry by lazy { NmsProvider.current.createPacketLoreRegistry() }

    @EventHandler
    fun onPluginDisable(event: PluginDisableEvent) {
        packetLoreRegistry.unregister(event.plugin)
    }
}