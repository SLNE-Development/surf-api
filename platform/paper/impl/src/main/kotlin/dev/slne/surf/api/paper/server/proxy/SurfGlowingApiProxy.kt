package dev.slne.surf.api.paper.server.proxy

import dev.slne.surf.api.paper.glow.SurfGlowingApi
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.common.NmsProvider
import org.springframework.stereotype.Service

@NmsUseWithCaution
@Service
class SurfGlowingApiProxy :
    SurfGlowingApi by NmsProvider.current.createGlowingApi()
