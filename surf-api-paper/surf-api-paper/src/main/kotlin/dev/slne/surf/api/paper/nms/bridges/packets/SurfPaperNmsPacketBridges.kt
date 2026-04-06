package dev.slne.surf.api.paper.nms.bridges.packets

import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.api.paper.nms.NmsUseWithCaution

@NmsUseWithCaution
interface SurfPaperNmsPacketBridges {
    fun createEmptyPacketOperation(): PacketOperation

    companion object : SurfPaperNmsPacketBridges by bridge {
        val INSTANCE get() = bridge
    }
}

@OptIn(NmsUseWithCaution::class)
private val bridge = requiredService<SurfPaperNmsPacketBridges>()