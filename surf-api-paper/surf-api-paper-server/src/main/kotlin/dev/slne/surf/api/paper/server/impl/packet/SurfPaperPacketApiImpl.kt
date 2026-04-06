package dev.slne.surf.api.paper.server.impl.packet

import com.google.auto.service.AutoService
import dev.slne.surf.api.core.util.checkInstantiationByServiceLoader
import dev.slne.surf.api.paper.packet.SurfPaperPacketApi
import dev.slne.surf.api.paper.packet.lore.SurfPaperPacketLoreHandler
import dev.slne.surf.api.paper.server.packet.lore.PacketLoreListener
import org.bukkit.NamespacedKey
import org.bukkit.plugin.Plugin

@AutoService(SurfPaperPacketApi::class)
class SurfPaperPacketApiImpl : SurfPaperPacketApi {
    init {
        checkInstantiationByServiceLoader()
    }

    override fun registerPacketLoreListener(
        plugin: Plugin,
        identifier: NamespacedKey,
        listener: SurfPaperPacketLoreHandler
    ) {
        PacketLoreListener.register(plugin, identifier, listener)
    }

    override fun registerPacketLoreListenerGlobal(
        plugin: Plugin,
        listener: SurfPaperPacketLoreHandler
    ) {
        PacketLoreListener.register(plugin, listener)
    }

    override fun unregisterPacketLoreListener(plugin: Plugin) {
        PacketLoreListener.unregister(plugin)
    }

    override fun unregisterPacketLoreListener(identifier: NamespacedKey) {
        PacketLoreListener.unregister(identifier)
    }
}
