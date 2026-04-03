package dev.slne.surf.surfapi.velocity.server.impl

import com.google.auto.service.AutoService
import com.velocitypowered.api.proxy.Player
import dev.slne.surf.surfapi.core.api.SurfApiCore
import dev.slne.surf.surfapi.core.api.util.checkInstantiationByServiceLoader
import dev.slne.surf.surfapi.core.server.impl.SurfApiCoreImpl
import dev.slne.surf.surfapi.velocity.api.SurfApiVelocity
import dev.slne.surf.surfapi.velocity.server.velocityMain
import java.util.*


@AutoService(SurfApiCore::class)
class SurfApiVelocityImpl : SurfApiCoreImpl(), SurfApiVelocity {
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
