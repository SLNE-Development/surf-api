package dev.slne.surf.api.paper.server.proxy

import com.google.auto.service.AutoService
import dev.slne.surf.api.core.util.checkInstantiationByServiceLoader
import dev.slne.surf.api.paper.glow.SurfGlowingApi
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.common.NmsProvider

@NmsUseWithCaution
@AutoService(SurfGlowingApi::class)
class SurfGlowingApiProxy :
    SurfGlowingApi by NmsProvider.current.createGlowingApi() {
    init { checkInstantiationByServiceLoader() }
}
