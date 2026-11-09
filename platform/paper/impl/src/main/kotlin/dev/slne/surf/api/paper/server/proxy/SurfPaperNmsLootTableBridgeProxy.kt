package dev.slne.surf.api.paper.server.proxy

import dev.slne.surf.api.di.Service
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsLootTableBridge
import dev.slne.surf.api.paper.nms.common.NmsProvider

@NmsUseWithCaution
@Service
class SurfPaperNmsLootTableBridgeProxy :
    SurfPaperNmsLootTableBridge by NmsProvider.current.createLootTableBridge()
