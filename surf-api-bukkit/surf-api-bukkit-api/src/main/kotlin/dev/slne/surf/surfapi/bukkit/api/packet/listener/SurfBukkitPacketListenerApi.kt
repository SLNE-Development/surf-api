package dev.slne.surf.surfapi.bukkit.api.packet.listener

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.PacketListener
import dev.slne.surf.surfapi.core.api.util.requiredService

@NmsUseWithCaution
interface SurfBukkitPacketListenerApi {
    fun registerListeners(listener: PacketListener)
    fun unregisterListeners(listener: PacketListener)

    companion object {
        val instance = requiredService<SurfBukkitPacketListenerApi>()
    }
}

@NmsUseWithCaution
val packetListenerApi get() = SurfBukkitPacketListenerApi.instance
