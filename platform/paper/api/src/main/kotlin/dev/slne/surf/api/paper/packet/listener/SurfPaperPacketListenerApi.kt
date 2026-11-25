package dev.slne.surf.api.paper.packet.listener

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.packet.listener.listener.PacketListener
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.api.spring.surfBean

@NmsUseWithCaution
interface SurfPaperPacketListenerApi {
    fun registerListeners(listener: PacketListener)
    fun unregisterListeners(listener: PacketListener)

    companion object {
        @InternalSurfApi
        val instance by surfBean<SurfPaperPacketListenerApi>()
    }
}
