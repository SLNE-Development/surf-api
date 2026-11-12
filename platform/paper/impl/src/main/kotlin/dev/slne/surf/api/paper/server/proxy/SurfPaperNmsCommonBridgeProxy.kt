package dev.slne.surf.api.paper.server.proxy

import dev.slne.surf.api.di.Service
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsCommonBridge
import dev.slne.surf.api.paper.nms.common.NmsProvider

/**
 * Version-aware proxy for [SurfPaperNmsCommonBridge].
 *
 * Published as a bean in the DI container, delegates to the NMS provider for the current server version.
 */
@NmsUseWithCaution
@Service
class SurfPaperNmsCommonBridgeProxy :
    SurfPaperNmsCommonBridge by NmsProvider.current.createCommonBridge()
