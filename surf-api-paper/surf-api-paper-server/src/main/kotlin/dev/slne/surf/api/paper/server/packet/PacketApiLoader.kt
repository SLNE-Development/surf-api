package dev.slne.surf.api.paper.server.packet

import com.github.retrooper.packetevents.PacketEvents
import dev.slne.surf.api.core.extensions.packetEvents
import dev.slne.surf.api.paper.event.register
import dev.slne.surf.api.paper.event.unregister
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.common.AbstractChannelInjector
import dev.slne.surf.api.paper.nms.common.NmsProvider
import dev.slne.surf.api.paper.packet.listener.SurfPaperPacketListenerApi
import dev.slne.surf.api.paper.packet.listener.listener.PacketListener
import dev.slne.surf.api.paper.server.packet.lore.PluginDisablePacketLoreListener
import dev.slne.surf.api.paper.server.plugin
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder

@OptIn(NmsUseWithCaution::class)
object PacketApiLoader {

    private var versionPacketListeners: List<PacketListener> = emptyList()

    fun onLoad() {
        setupPacketEvents()
    }

    @OptIn(NmsUseWithCaution::class)
    fun onEnable() {
        packetEvents.init()

        // Register version-specific packet listeners from NmsProvider
        val provider = NmsProvider.current
        provider.initialize()
        
        versionPacketListeners = provider.createPacketListeners()
        for (listener in versionPacketListeners) {
            SurfPaperPacketListenerApi.registerListeners(listener)
        }

        AbstractChannelInjector.instance.register()
        PluginDisablePacketLoreListener.register()
    }

    @OptIn(NmsUseWithCaution::class)
    fun onDisable() {
        val provider = NmsProvider.current

        packetEvents.terminate()
        for (listener in versionPacketListeners) {
            SurfPaperPacketListenerApi.unregisterListeners(listener)
        }
        versionPacketListeners = emptyList()

        PluginDisablePacketLoreListener.unregister()
        AbstractChannelInjector.instance.unregister()
        provider.shutdown()
    }

    private fun setupPacketEvents() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(plugin))
        packetEvents.load()
    }
}
