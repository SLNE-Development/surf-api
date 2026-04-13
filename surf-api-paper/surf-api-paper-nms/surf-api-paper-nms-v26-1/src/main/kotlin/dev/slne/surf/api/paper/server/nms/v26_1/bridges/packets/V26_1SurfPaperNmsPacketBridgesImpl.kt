package dev.slne.surf.api.paper.server.nms.v26_1.bridges.packets

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.SurfPaperNmsPacketBridges

@NmsUseWithCaution
class V26_1SurfPaperNmsPacketBridgesImpl : SurfPaperNmsPacketBridges {

    override fun createEmptyPacketOperation(): V26_1PacketOperationImpl {
        return V26_1PacketOperationImpl.empty()
    }
}
