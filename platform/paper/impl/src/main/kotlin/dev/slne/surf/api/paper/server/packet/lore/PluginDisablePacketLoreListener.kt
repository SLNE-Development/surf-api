package dev.slne.surf.api.paper.server.packet.lore

import dev.slne.surf.api.paper.event.BukkitListener
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.common.PacketLoreRegistry
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginDisableEvent

@NmsUseWithCaution
@BukkitListener
internal class PluginDisablePacketLoreListener(
    private val packetLoreRegistry: PacketLoreRegistry,
) : Listener {

    @EventHandler
    fun onPluginDisable(event: PluginDisableEvent) {
        packetLoreRegistry.unregister(event.plugin)
    }
}
