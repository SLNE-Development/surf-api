package dev.slne.surf.api.paper.server.proxy

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsCommonBridge
import dev.slne.surf.api.paper.nms.common.NmsProvider
import org.springframework.stereotype.Service

/**
 * Version-aware proxy for [SurfPaperNmsCommonBridge].
 *
 * Delegates to the bridge of the NMS provider for the running server version.
 */
@NmsUseWithCaution
@Service
class SurfPaperNmsCommonBridgeProxy :
    SurfPaperNmsCommonBridge by NmsProvider.current.createCommonBridge()
