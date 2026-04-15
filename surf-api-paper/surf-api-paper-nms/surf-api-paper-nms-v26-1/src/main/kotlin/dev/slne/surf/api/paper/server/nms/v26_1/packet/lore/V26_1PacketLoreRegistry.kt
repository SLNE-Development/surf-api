package dev.slne.surf.api.paper.server.nms.v26_1.packet.lore

import dev.slne.surf.api.paper.nms.common.PacketLoreRegistry
import dev.slne.surf.api.paper.packet.lore.SurfPaperPacketLoreHandler
import org.bukkit.NamespacedKey
import org.bukkit.plugin.Plugin

@Suppress("ClassName")
class V26_1PacketLoreRegistry : PacketLoreRegistry {
    override fun register(plugin: Plugin, identifier: NamespacedKey, listener: SurfPaperPacketLoreHandler) {
        V26_1PacketLoreListener.register(plugin, identifier, listener)
    }

    override fun register(plugin: Plugin, listener: SurfPaperPacketLoreHandler) {
        V26_1PacketLoreListener.register(plugin, listener)
    }

    override fun unregister(identifier: NamespacedKey) {
        V26_1PacketLoreListener.unregister(identifier)
    }

    override fun unregister(plugin: Plugin) {
        V26_1PacketLoreListener.unregister(plugin)
    }
}
