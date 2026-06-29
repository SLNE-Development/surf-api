package dev.slne.surf.api.paper.server.nms.v26_2.bridges.packets

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.SurfPaperNmsPacketBridges

@NmsUseWithCaution
@Suppress("ClassName")
class V26_2SurfPaperNmsPacketBridgesImpl : SurfPaperNmsPacketBridges {

    override fun createEmptyPacketOperation(): V26_2PacketOperationImpl {
        return V26_2PacketOperationImpl.empty()
    }
}
