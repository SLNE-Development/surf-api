package dev.slne.surf.api.paper.server.proxy

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.common.NmsProvider
import dev.slne.surf.api.paper.region.TickThreadGuard
import org.springframework.stereotype.Service

@NmsUseWithCaution
@Service
class TickThreadGuardProxy :
    TickThreadGuard by NmsProvider.current.createTickThreadGuard()
