package dev.slne.surf.api.paper.server.proxy

import com.google.auto.service.AutoService
import dev.slne.surf.api.core.util.checkInstantiationByServiceLoader
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.common.NmsProvider
import dev.slne.surf.api.paper.packet.listener.SurfPaperPacketListenerApi

@NmsUseWithCaution
@AutoService(SurfPaperPacketListenerApi::class)
class SurfPaperPacketListenerApiProxy : SurfPaperPacketListenerApi by NmsProvider.current.createPacketListenerApi() {
    init {
        checkInstantiationByServiceLoader()
    }
}