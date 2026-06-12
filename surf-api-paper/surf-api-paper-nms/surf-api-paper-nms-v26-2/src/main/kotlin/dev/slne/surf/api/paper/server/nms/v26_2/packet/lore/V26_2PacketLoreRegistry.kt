package dev.slne.surf.api.paper.server.nms.v26_2.packet.lore

import dev.slne.surf.api.paper.nms.common.PacketLoreRegistry
import dev.slne.surf.api.paper.packet.lore.SurfPaperPacketLoreHandler
import org.bukkit.NamespacedKey
import org.bukkit.plugin.Plugin

@Suppress("ClassName")
class V26_2PacketLoreRegistry : PacketLoreRegistry {
    override fun register(
        plugin: Plugin,
        identifier: NamespacedKey,
        listener: SurfPaperPacketLoreHandler,
        priority: Short
    ) {
        V26_2PacketLoreListener.register(plugin, identifier, listener, priority)
    }

    override fun register(plugin: Plugin, listener: SurfPaperPacketLoreHandler, priority: Short) {
        V26_2PacketLoreListener.register(plugin, listener, priority)
    }

    override fun unregister(identifier: NamespacedKey) {
        V26_2PacketLoreListener.unregister(identifier)
    }

    override fun unregister(plugin: Plugin) {
        V26_2PacketLoreListener.unregister(plugin)
    }
}
