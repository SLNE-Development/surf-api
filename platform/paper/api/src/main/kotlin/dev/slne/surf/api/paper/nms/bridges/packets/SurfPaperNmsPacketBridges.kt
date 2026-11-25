package dev.slne.surf.api.paper.nms.bridges.packets

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.spring.surfBean

@NmsUseWithCaution
interface SurfPaperNmsPacketBridges {
    fun createEmptyPacketOperation(): PacketOperation

    companion object {
        internal val instance by surfBean<SurfPaperNmsPacketBridges>()
    }
}
