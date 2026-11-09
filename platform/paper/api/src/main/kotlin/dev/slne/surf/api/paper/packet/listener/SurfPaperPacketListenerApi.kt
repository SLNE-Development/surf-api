@file:OptIn(InternalSurfApi::class)

package dev.slne.surf.api.paper.packet.listener

import dev.slne.surf.api.di.surfBean
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.packet.listener.listener.PacketListener
import dev.slne.surf.api.shared.api.util.InternalSurfApi

@NmsUseWithCaution
interface SurfPaperPacketListenerApi {
    fun registerListeners(listener: PacketListener)
    fun unregisterListeners(listener: PacketListener)

    companion object {
        val INSTANCE get() = api
    }
}

@NmsUseWithCaution
private val api by surfBean<SurfPaperPacketListenerApi>()
