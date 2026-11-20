package dev.slne.surf.api.paper.server.proxy

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.player.SurfPaperNmsPlayerToastPackets
import dev.slne.surf.api.paper.nms.common.NmsProvider
import org.springframework.stereotype.Service

@NmsUseWithCaution
@Service
class SurfPaperNmsPlayerToastPacketsProxy :
    SurfPaperNmsPlayerToastPackets by NmsProvider.current.createPlayerToastPackets()
