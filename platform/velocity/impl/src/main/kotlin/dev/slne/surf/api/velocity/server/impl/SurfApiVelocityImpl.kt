package dev.slne.surf.api.velocity.server.impl

import com.velocitypowered.api.proxy.Player
import dev.slne.surf.api.core.server.impl.SurfApiCoreImpl
import dev.slne.surf.api.di.Service
import dev.slne.surf.api.velocity.SurfApiVelocity
import dev.slne.surf.api.velocity.server.plugin
import net.kyori.adventure.audience.Audience
import java.util.*

@Service
class SurfApiVelocityImpl : SurfApiCoreImpl(), SurfApiVelocity {

    override val executorService get() = plugin.executorService

    override fun sendPlayerToServer(playerUuid: UUID, server: String) {
        val proxy = plugin.server
        proxy.getPlayer(playerUuid).ifPresent { player ->
            proxy.getServer(server).ifPresent { server -> player.createConnectionRequest(server) }
        }
    }

    override fun getPlayer(playerUuid: UUID): Player? = plugin.server.getPlayer(playerUuid).orElse(null)
    override fun isPlayer(audience: Audience): Boolean {
        return audience is Player
    }
}
