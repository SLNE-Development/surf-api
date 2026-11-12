package dev.slne.surf.api.minestom.server.impl

import dev.slne.surf.api.core.server.impl.SurfApiCoreImpl
import dev.slne.surf.api.di.Service
import net.kyori.adventure.audience.Audience
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.network.NetworkBuffer
import java.util.*


@Service
internal class SurfMinestomApiImpl : SurfApiCoreImpl() {

    override fun isPlayer(audience: Audience): Boolean {
        return audience is Player
    }

    override fun sendPlayerToServer(playerUuid: UUID, server: String) {
        MinecraftServer
            .getConnectionManager()
            .getOnlinePlayerByUuid(playerUuid)
            ?.sendPluginMessage(
                "bungeecord:main",
                NetworkBuffer.makeArray { buffer ->
                    buffer.write(NetworkBuffer.STRING_IO_UTF8, "Connect")
                    buffer.write(NetworkBuffer.STRING_IO_UTF8, server)
                }
            )
    }

    override fun getPlayer(playerUuid: UUID): Any? {
        return MinecraftServer
            .getConnectionManager()
            .getOnlinePlayerByUuid(playerUuid)
    }
}