package dev.slne.surf.surfapi.bukkit.server.packet

import com.github.retrooper.packetevents.PacketEvents
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.packet.listener.packetListenerApi
import dev.slne.surf.surfapi.bukkit.server.impl.glow.GlowingPacketListener
import dev.slne.surf.surfapi.bukkit.server.packet.lore.PacketLoreListener
import dev.slne.surf.surfapi.bukkit.server.plugin
import dev.slne.surf.surfapi.core.api.extensions.packetEvents
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder

object PacketApiLoader {

    fun onLoad() {
        setupPacketEvents()
    }

    @OptIn(NmsUseWithCaution::class)
    fun onEnable() {
        packetEvents.init()
        packetListenerApi.registerListeners(PacketLoreListener)
        packetListenerApi.registerListeners(GlowingPacketListener)

//        PlayerChannelInjector.register()
    }

    @OptIn(NmsUseWithCaution::class)
    fun onDisable() {
        packetEvents.terminate()
        packetListenerApi.unregisterListeners(PacketLoreListener)
    }

    private fun setupPacketEvents() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(plugin))
        packetEvents.load()
    }
}
