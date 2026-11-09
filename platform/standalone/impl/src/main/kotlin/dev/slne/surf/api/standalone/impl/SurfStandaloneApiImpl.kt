package dev.slne.surf.api.standalone.impl

import dev.slne.surf.api.core.server.impl.SurfApiCoreImpl
import dev.slne.surf.api.di.Service
import net.kyori.adventure.audience.Audience
import java.nio.file.Path
import java.util.*

@Service
class SurfStandaloneApiImpl : SurfApiCoreImpl() {

    override fun sendPlayerToServer(playerUuid: UUID, server: String) {
        throw UnsupportedOperationException("sendPlayerToServer is not supported in standalone mode")
    }

    override fun getPlayer(playerUuid: UUID): Any? {
        throw UnsupportedOperationException("getPlayer is not supported in standalone mode")
    }

    override fun isPlayer(audience: Audience): Boolean {
        return false
    }

    val dataFolder: Path get() = Path.of("api-data")
}
