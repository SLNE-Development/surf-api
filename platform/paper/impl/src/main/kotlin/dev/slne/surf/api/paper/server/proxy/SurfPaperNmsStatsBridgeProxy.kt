package dev.slne.surf.api.paper.server.proxy

import com.google.auto.service.AutoService
import dev.slne.surf.api.core.util.checkInstantiationByServiceLoader
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsStatsBridge
import dev.slne.surf.api.paper.nms.common.NmsProvider

@NmsUseWithCaution
@AutoService(SurfPaperNmsStatsBridge::class)
class SurfPaperNmsStatsBridgeProxy :
    SurfPaperNmsStatsBridge by NmsProvider.current.createStatsBridge() {
    init { checkInstantiationByServiceLoader() }
}
