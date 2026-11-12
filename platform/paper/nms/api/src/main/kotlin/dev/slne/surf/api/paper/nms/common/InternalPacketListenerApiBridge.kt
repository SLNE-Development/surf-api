package dev.slne.surf.api.paper.nms.common

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.packet.listener.SurfPaperPacketListenerApi

@NmsUseWithCaution
interface InternalPacketListenerApiBridge : SurfPaperPacketListenerApi {
    fun handleClientboundPacket(
        packet: Any,
        serverPlayer: Any?
    ): Any?

    fun handleServerboundPacket(
        packet: Any,
        serverPlayer: Any?
    ): Any?

    companion object {
        fun get() = SurfPaperPacketListenerApi.INSTANCE as InternalPacketListenerApiBridge
    }
}

