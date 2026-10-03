package dev.slne.surf.api.paper.server.nms.v26_3.bridges.packets

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.SurfPaperNmsPacketBridges

@NmsUseWithCaution
@Suppress("ClassName")
class V26_3SurfPaperNmsPacketBridgesImpl : SurfPaperNmsPacketBridges {

    override fun createEmptyPacketOperation(): V26_3PacketOperationImpl {
        return V26_3PacketOperationImpl.empty()
    }
}
