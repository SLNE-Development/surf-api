package dev.slne.surf.api.paper.server.proxy

import com.google.auto.service.AutoService
import dev.slne.surf.api.core.util.checkInstantiationByServiceLoader
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsCommonBridge
import dev.slne.surf.api.paper.nms.common.NmsProvider

/**
 * Version-aware proxy for [SurfPaperNmsCommonBridge].
 *
 * Loaded via ServiceLoader, delegates to the NMS provider for the current server version.
 */
@NmsUseWithCaution
@AutoService(SurfPaperNmsCommonBridge::class)
class SurfPaperNmsCommonBridgeProxy :
    SurfPaperNmsCommonBridge by NmsProvider.current.createCommonBridge() {
    init {
        checkInstantiationByServiceLoader()
    }
}
