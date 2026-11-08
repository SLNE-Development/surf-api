package dev.slne.surf.api.paper.nms

import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.api.paper.api.nms.listener.NmsClientboundPacketListener
import dev.slne.surf.api.paper.api.nms.listener.NmsServerboundPacketListener

@NmsUseWithCaution
interface SurfPaperNmsBridge {

    fun registerServerboundPacketListener(listener: NmsServerboundPacketListener<*>)
    fun unregisterServerboundPacketListener(listener: NmsServerboundPacketListener<*>)

    fun registerClientboundPacketListener(listener: NmsClientboundPacketListener<*>)
    fun unregisterClientboundPacketListener(listener: NmsClientboundPacketListener<*>)

    companion object : SurfPaperNmsBridge by bridge {
        val INSTANCE get() = bridge
    }
}

@NmsUseWithCaution
private val bridge = requiredService<SurfPaperNmsBridge>()
