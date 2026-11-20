package dev.slne.surf.api.paper.server.proxy

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsCommandArgumentTypesBridge
import dev.slne.surf.api.paper.nms.common.NmsProvider
import org.springframework.stereotype.Service

@NmsUseWithCaution
@Service
class SurfPaperNmsCommandArgumentTypesBridgeProxy :
    SurfPaperNmsCommandArgumentTypesBridge by NmsProvider.current.createCommandArgumentTypesBridge()
