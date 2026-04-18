package dev.slne.surf.api.paper.server.proxy

import com.google.auto.service.AutoService
import dev.slne.surf.api.core.util.checkInstantiationByServiceLoader
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsNbtBridge
import dev.slne.surf.api.paper.nms.common.NmsProvider

@NmsUseWithCaution
@AutoService(SurfPaperNmsNbtBridge::class)
class SurfPaperNmsNbtBridgeProxy :
    SurfPaperNmsNbtBridge by NmsProvider.current.createNbtBridge() {
    init { checkInstantiationByServiceLoader() }
}
