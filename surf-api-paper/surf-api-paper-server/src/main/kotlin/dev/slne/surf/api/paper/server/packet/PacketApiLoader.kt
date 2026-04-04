package dev.slne.surf.api.paper.server.packet

import com.github.retrooper.packetevents.PacketEvents
import dev.slne.surf.api.core.extensions.packetEvents
import dev.slne.surf.api.paper.event.register
import dev.slne.surf.api.paper.event.unregister
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.packet.listener.SurfPaperPacketListenerApi
import dev.slne.surf.api.paper.server.impl.glow.GlowingPacketListener
import dev.slne.surf.api.paper.server.packet.listener.PlayerChannelInjector
import dev.slne.surf.api.paper.server.packet.lore.PacketLoreListener
import dev.slne.surf.api.paper.server.packet.lore.PluginDisablePacketLoreListener
import dev.slne.surf.api.paper.server.plugin
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder

object PacketApiLoader {

    fun onLoad() {
        setupPacketEvents()
    }

    @OptIn(NmsUseWithCaution::class)
    fun onEnable() {
        packetEvents.init()
        SurfPaperPacketListenerApi.registerListeners(PacketLoreListener)
        SurfPaperPacketListenerApi.registerListeners(GlowingPacketListener)

        PlayerChannelInjector.register()
        PluginDisablePacketLoreListener.register()
    }

    @OptIn(NmsUseWithCaution::class)
    fun onDisable() {
        packetEvents.terminate()
        SurfPaperPacketListenerApi.unregisterListeners(PacketLoreListener)
        PluginDisablePacketLoreListener.unregister()
        PlayerChannelInjector.unregister()
    }

    private fun setupPacketEvents() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(plugin))
        packetEvents.load()
    }
}
