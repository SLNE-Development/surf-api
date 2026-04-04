package dev.slne.surf.api.paper.server.impl.nms.bridges.packets

import com.google.auto.service.AutoService
import dev.slne.surf.api.core.util.checkInstantiationByServiceLoader
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.SurfPaperNmsPacketBridges

@NmsUseWithCaution
@AutoService(SurfPaperNmsPacketBridges::class)
class SurfPaperNmsPacketBridgesImpl : SurfPaperNmsPacketBridges {
    init {
        checkInstantiationByServiceLoader()
    }

    override fun createEmptyPacketOperation(): PacketOperationImpl {
        return PacketOperationImpl.empty()
    }
}
