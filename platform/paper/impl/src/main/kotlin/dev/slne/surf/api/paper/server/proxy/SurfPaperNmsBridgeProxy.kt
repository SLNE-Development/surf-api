package dev.slne.surf.api.paper.server.proxy

import dev.slne.surf.api.di.Service
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.common.InternalNmsBridge
import dev.slne.surf.api.paper.nms.common.NmsProvider

@NmsUseWithCaution
@Service
class SurfPaperNmsBridgeProxy : InternalNmsBridge by NmsProvider.current.createNmsBridge()
