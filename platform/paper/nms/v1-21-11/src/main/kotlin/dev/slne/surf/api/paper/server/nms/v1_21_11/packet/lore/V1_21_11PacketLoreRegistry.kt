package dev.slne.surf.api.paper.server.nms.v1_21_11.packet.lore

import dev.slne.surf.api.paper.nms.common.NmsComponent
import dev.slne.surf.api.paper.nms.common.PacketLoreRegistry
import dev.slne.surf.api.paper.packet.lore.SurfPaperPacketLoreHandler
import org.bukkit.NamespacedKey
import org.bukkit.plugin.Plugin

@NmsComponent
class V1_21_11PacketLoreRegistry(
    private val loreListener: V1_21_11PacketLoreListener,
) : PacketLoreRegistry {
    override fun register(
        plugin: Plugin,
        identifier: NamespacedKey,
        listener: SurfPaperPacketLoreHandler,
        priority: Short,
    ) {
        loreListener.register(plugin, identifier, listener, priority)
    }

    override fun register(plugin: Plugin, listener: SurfPaperPacketLoreHandler, priority: Short) {
        loreListener.register(plugin, listener, priority)
    }

    override fun unregister(identifier: NamespacedKey) {
        loreListener.unregister(identifier)
    }

    override fun unregister(plugin: Plugin) {
        loreListener.unregister(plugin)
    }
}
