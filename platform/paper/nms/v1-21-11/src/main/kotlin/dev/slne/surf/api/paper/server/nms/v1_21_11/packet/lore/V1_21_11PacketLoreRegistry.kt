package dev.slne.surf.api.paper.server.nms.v1_21_11.packet.lore

import dev.slne.surf.api.paper.nms.common.PacketLoreRegistry
import dev.slne.surf.api.paper.packet.lore.SurfPaperPacketLoreHandler
import org.bukkit.NamespacedKey
import org.bukkit.plugin.Plugin

class V1_21_11PacketLoreRegistry : PacketLoreRegistry {
    override fun register(plugin: Plugin, identifier: NamespacedKey, listener: SurfPaperPacketLoreHandler, priority: Short) {
        V1_21_11PacketLoreListener.register(plugin, identifier, listener, priority)
    }

    override fun register(plugin: Plugin, listener: SurfPaperPacketLoreHandler, priority: Short) {
        V1_21_11PacketLoreListener.register(plugin, listener, priority)
    }

    override fun unregister(identifier: NamespacedKey) {
        V1_21_11PacketLoreListener.unregister(identifier)
    }

    override fun unregister(plugin: Plugin) {
        V1_21_11PacketLoreListener.unregister(plugin)
    }
}
