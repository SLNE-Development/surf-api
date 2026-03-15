package dev.slne.surf.surfapi.bukkit.server.impl.packet

import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.bukkit.api.packet.SurfBukkitPacketApi
import dev.slne.surf.surfapi.bukkit.api.packet.lore.SurfBukkitPacketLoreHandler
import dev.slne.surf.surfapi.bukkit.server.packet.lore.PacketLoreListener
import dev.slne.surf.surfapi.core.api.util.checkInstantiationByServiceLoader
import org.bukkit.NamespacedKey
import org.bukkit.plugin.Plugin

@AutoService(SurfBukkitPacketApi::class)
class SurfBukkitPacketApiImpl : SurfBukkitPacketApi {
    init {
        checkInstantiationByServiceLoader()
    }

    override fun registerPacketLoreListener(
        plugin: Plugin,
        identifier: NamespacedKey,
        listener: SurfBukkitPacketLoreHandler
    ) {
        PacketLoreListener.register(plugin, identifier, listener)
    }

    override fun registerPacketLoreListenerGlobal(
        plugin: Plugin,
        listener: SurfBukkitPacketLoreHandler
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
