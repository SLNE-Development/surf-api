@file:OptIn(InternalSurfApi::class)

package dev.slne.surf.api.paper.nms.bridges.packets

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.api.spring.surfBean

@NmsUseWithCaution
interface SurfPaperNmsPacketBridges {
    fun createEmptyPacketOperation(): PacketOperation

    companion object {
        internal val INSTANCE get() = bridge
    }
}

@OptIn(NmsUseWithCaution::class)
private val bridge by surfBean<SurfPaperNmsPacketBridges>()
