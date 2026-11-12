@file:OptIn(InternalSurfApi::class)

package dev.slne.surf.api.paper.nms.bridges.packets

import dev.slne.surf.api.di.surfBean
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.shared.api.util.InternalSurfApi

@NmsUseWithCaution
interface SurfPaperNmsPacketBridges {
    fun createEmptyPacketOperation(): PacketOperation

    companion object {
        val INSTANCE get() = bridge
    }
}

@OptIn(NmsUseWithCaution::class)
private val bridge by surfBean<SurfPaperNmsPacketBridges>()
