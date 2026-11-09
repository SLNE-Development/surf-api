package dev.slne.surf.api.paper.server.proxy

import dev.slne.surf.api.di.Service
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsNbtBridge
import dev.slne.surf.api.paper.nms.common.NmsProvider

@NmsUseWithCaution
@Service
class SurfPaperNmsNbtBridgeProxy :
    SurfPaperNmsNbtBridge by NmsProvider.current.createNbtBridge()
