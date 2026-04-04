package dev.slne.surf.api.velocity.server.impl

import com.google.auto.service.AutoService
import com.velocitypowered.api.proxy.Player
import dev.slne.surf.api.core.SurfApiCore
import dev.slne.surf.api.core.server.impl.SurfApiCoreImpl
import dev.slne.surf.api.core.util.checkInstantiationByServiceLoader
import dev.slne.surf.api.velocity.SurfApiVelocity
import dev.slne.surf.api.velocity.server.plugin
import java.util.*

@AutoService(SurfApiCore::class)
class SurfApiVelocityImpl : SurfApiCoreImpl(), SurfApiVelocity {
    init {
        checkInstantiationByServiceLoader()
    }

    override val executorService get() = plugin.executorService

    override fun sendPlayerToServer(playerUuid: UUID, server: String) {

        val proxy = plugin.server
        proxy.getPlayer(playerUuid).ifPresent { player ->
            proxy.getServer(server).ifPresent { server -> player.createConnectionRequest(server) }
        }
    }

    override fun getPlayer(playerUuid: UUID): Player =
        plugin.server.getPlayer(playerUuid).orElse(null)
}
