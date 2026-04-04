package dev.slne.surf.api.paper.packet.listener

import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.packet.listener.listener.PacketListener

@NmsUseWithCaution
interface SurfPaperPacketListenerApi {
    fun registerListeners(listener: PacketListener)
    fun unregisterListeners(listener: PacketListener)

    companion object : SurfPaperPacketListenerApi by api {
        val INSTANCE get() = api
    }
}

@NmsUseWithCaution
private val api = requiredService<SurfPaperPacketListenerApi>()
