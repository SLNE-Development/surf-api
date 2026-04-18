package dev.slne.surf.api.paper.server.nms.v1_21_11.bridges.packets

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.SurfPaperNmsPacketBridges

@NmsUseWithCaution
class V1_21_11SurfPaperNmsPacketBridgesImpl : SurfPaperNmsPacketBridges {

    override fun createEmptyPacketOperation(): V1_21_11PacketOperationImpl {
        return V1_21_11PacketOperationImpl.empty()
    }
}
