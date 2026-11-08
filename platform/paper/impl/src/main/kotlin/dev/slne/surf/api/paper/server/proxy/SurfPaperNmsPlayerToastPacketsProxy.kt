package dev.slne.surf.api.paper.server.proxy

import com.google.auto.service.AutoService
import dev.slne.surf.api.core.util.checkInstantiationByServiceLoader
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.player.SurfPaperNmsPlayerToastPackets
import dev.slne.surf.api.paper.nms.common.NmsProvider

@NmsUseWithCaution
@AutoService(SurfPaperNmsPlayerToastPackets::class)
class SurfPaperNmsPlayerToastPacketsProxy :
    SurfPaperNmsPlayerToastPackets by NmsProvider.current.createPlayerToastPackets() {
    init { checkInstantiationByServiceLoader() }
}
