package dev.slne.surf.api.paper.server.proxy

import dev.slne.surf.api.di.Service
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsStatsBridge
import dev.slne.surf.api.paper.nms.common.NmsProvider

@NmsUseWithCaution
@Service
class SurfPaperNmsStatsBridgeProxy :
    SurfPaperNmsStatsBridge by NmsProvider.current.createStatsBridge()
