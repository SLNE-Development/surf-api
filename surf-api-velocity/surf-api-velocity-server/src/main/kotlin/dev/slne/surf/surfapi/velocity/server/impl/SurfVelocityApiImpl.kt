package dev.slne.surf.surfapi.velocity.server.impl

import com.google.auto.service.AutoService
import com.velocitypowered.api.proxy.Player
import dev.slne.surf.surfapi.core.api.SurfCoreApi
import dev.slne.surf.surfapi.core.api.util.checkInstantiationByServiceLoader
import dev.slne.surf.surfapi.core.server.impl.SurfCoreApiImpl
import dev.slne.surf.surfapi.velocity.api.SurfVelocityApi
import dev.slne.surf.surfapi.velocity.server.velocityMain
import java.util.*


@AutoService(SurfCoreApi::class)
class SurfVelocityApiImpl : SurfCoreApiImpl(), SurfVelocityApi {
    init {
        checkInstantiationByServiceLoader()
    }

    override val executorService get() = velocityMain.executorService

    override fun sendPlayerToServer(playerUuid: UUID, server: String) {

        val proxy = velocityMain.server
        proxy.getPlayer(playerUuid).ifPresent { player ->
            proxy.getServer(server).ifPresent { server -> player.createConnectionRequest(server) }
        }
    }

    override fun getPlayer(playerUuid: UUID): Player =
        velocityMain.server.getPlayer(playerUuid).orElse(null)
}
