package dev.slne.surf.api.paper.server.proxy

import com.google.auto.service.AutoService
import dev.slne.surf.api.core.util.checkInstantiationByServiceLoader
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.entity.SurfPaperNmsSpawnPackets
import dev.slne.surf.api.paper.nms.common.NmsProvider

@NmsUseWithCaution
@AutoService(SurfPaperNmsSpawnPackets::class)
class SurfPaperNmsSpawnPacketsProxy :
    SurfPaperNmsSpawnPackets by NmsProvider.current.createSpawnPackets() {
    init { checkInstantiationByServiceLoader() }
}
