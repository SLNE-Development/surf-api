package dev.slne.surf.api.paper.server.impl.packet

import com.google.auto.service.AutoService
import dev.slne.surf.api.core.util.checkInstantiationByServiceLoader
import dev.slne.surf.api.paper.nms.common.NmsProvider
import dev.slne.surf.api.paper.nms.common.PacketLoreRegistry
import dev.slne.surf.api.paper.packet.SurfPaperPacketApi
import dev.slne.surf.api.paper.packet.lore.SurfPaperPacketLoreHandler
import org.bukkit.NamespacedKey
import org.bukkit.plugin.Plugin

@AutoService(SurfPaperPacketApi::class)
class SurfPaperPacketApiImpl : SurfPaperPacketApi {
    private val packetLoreRegistry: PacketLoreRegistry = NmsProvider.current.createPacketLoreRegistry()

    init {
        checkInstantiationByServiceLoader()
    }

    override fun registerPacketLoreListener(
        plugin: Plugin,
        identifier: NamespacedKey,
        listener: SurfPaperPacketLoreHandler
    ) {
        packetLoreRegistry.register(plugin, identifier, listener)
    }

    override fun registerPacketLoreListenerGlobal(
        plugin: Plugin,
        listener: SurfPaperPacketLoreHandler
    ) {
        packetLoreRegistry.register(plugin, listener)
    }

    override fun unregisterPacketLoreListener(plugin: Plugin) {
        packetLoreRegistry.unregister(plugin)
    }

    override fun unregisterPacketLoreListener(identifier: NamespacedKey) {
        packetLoreRegistry.unregister(identifier)
    }
}
