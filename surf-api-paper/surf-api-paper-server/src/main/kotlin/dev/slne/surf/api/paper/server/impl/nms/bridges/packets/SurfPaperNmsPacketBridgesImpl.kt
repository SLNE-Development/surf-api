package dev.slne.surf.api.paper.server.impl.nms.bridges.packets

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.SurfPaperNmsPacketBridges

@NmsUseWithCaution
class SurfPaperNmsPacketBridgesImpl : SurfPaperNmsPacketBridges {
    init {
    }

    override fun createEmptyPacketOperation(): PacketOperationImpl {
        return PacketOperationImpl.empty()
    }
}
