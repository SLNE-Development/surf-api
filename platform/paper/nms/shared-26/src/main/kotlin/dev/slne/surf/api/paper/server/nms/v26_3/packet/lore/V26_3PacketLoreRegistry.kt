package dev.slne.surf.api.paper.server.nms.v26_3.packet.lore

import dev.slne.surf.api.paper.nms.common.NmsComponent
import dev.slne.surf.api.paper.nms.common.PacketLoreRegistry
import dev.slne.surf.api.paper.packet.lore.SurfPaperPacketLoreHandler
import org.bukkit.NamespacedKey
import org.bukkit.plugin.Plugin

@Suppress("ClassName")
@NmsComponent
class V26_3PacketLoreRegistry(
    private val loreListener: V26_3PacketLoreListener,
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
