@file:OptIn(InternalSurfApi::class)

package dev.slne.surf.api.paper.nms

import dev.slne.surf.api.di.surfBean
import dev.slne.surf.api.paper.api.nms.listener.NmsClientboundPacketListener
import dev.slne.surf.api.paper.api.nms.listener.NmsServerboundPacketListener
import dev.slne.surf.api.shared.api.util.InternalSurfApi

@NmsUseWithCaution
interface SurfPaperNmsBridge {

    fun registerServerboundPacketListener(listener: NmsServerboundPacketListener<*>)
    fun unregisterServerboundPacketListener(listener: NmsServerboundPacketListener<*>)

    fun registerClientboundPacketListener(listener: NmsClientboundPacketListener<*>)
    fun unregisterClientboundPacketListener(listener: NmsClientboundPacketListener<*>)

    companion object {
        val INSTANCE get() = bridge
    }
}

@NmsUseWithCaution
private val bridge by surfBean<SurfPaperNmsBridge>()
