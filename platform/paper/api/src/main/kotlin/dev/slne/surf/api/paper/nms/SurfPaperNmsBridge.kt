@file:OptIn(InternalSurfApi::class)

package dev.slne.surf.api.paper.nms

import dev.slne.surf.api.paper.api.nms.listener.NmsClientboundPacketListener
import dev.slne.surf.api.paper.api.nms.listener.NmsServerboundPacketListener
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.api.spring.surfBean

@NmsUseWithCaution
interface SurfPaperNmsBridge {

    fun registerServerboundPacketListener(listener: NmsServerboundPacketListener<*>)
    fun unregisterServerboundPacketListener(listener: NmsServerboundPacketListener<*>)

    fun registerClientboundPacketListener(listener: NmsClientboundPacketListener<*>)
    fun unregisterClientboundPacketListener(listener: NmsClientboundPacketListener<*>)

    companion object {
        @InternalSurfApi
        val INSTANCE get() = bridge
    }
}

@NmsUseWithCaution
private val bridge by surfBean<SurfPaperNmsBridge>()
