package dev.slne.surf.api.paper.server.proxy

import com.google.auto.service.AutoService
import dev.slne.surf.api.core.util.checkInstantiationByServiceLoader
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsPlayerBridge
import dev.slne.surf.api.paper.nms.common.NmsProvider

@NmsUseWithCaution
@AutoService(SurfPaperNmsPlayerBridge::class)
class SurfPaperNmsPlayerBridgeProxy : SurfPaperNmsPlayerBridge by NmsProvider.current.createPlayerBridge() {
    init {
        checkInstantiationByServiceLoader()
    }
}