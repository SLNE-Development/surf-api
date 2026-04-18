package dev.slne.surf.api.paper.server.proxy

import com.google.auto.service.AutoService
import dev.slne.surf.api.core.util.checkInstantiationByServiceLoader
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.block.SurfPaperNmsBlockPackets
import dev.slne.surf.api.paper.nms.common.NmsProvider

@NmsUseWithCaution
@AutoService(SurfPaperNmsBlockPackets::class)
class SurfPaperNmsBlockPacketsProxy :
    SurfPaperNmsBlockPackets by NmsProvider.current.createBlockPackets() {
    init { checkInstantiationByServiceLoader() }
}
