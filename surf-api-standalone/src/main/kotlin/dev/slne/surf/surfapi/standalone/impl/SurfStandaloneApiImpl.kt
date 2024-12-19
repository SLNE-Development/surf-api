package dev.slne.surf.surfapi.standalone.impl

import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.core.api.SurfCoreApi
import dev.slne.surf.surfapi.core.api.util.checkInstantiationByServiceLoader
import dev.slne.surf.surfapi.core.server.impl.SurfCoreApiImpl
import java.nio.file.Path
import java.util.*

@AutoService(SurfCoreApi::class)
class SurfStandaloneApiImpl : SurfCoreApiImpl() {
    init {
        checkInstantiationByServiceLoader()
    }

    override fun sendPlayerToServer(playerUuid: UUID, server: String) {
        throw UnsupportedOperationException("sendPlayerToServer is not supported in standalone mode")
    }

    override fun getPlayer(playerUuid: UUID): Any? {
        throw UnsupportedOperationException("getPlayer is not supported in standalone mode")
    }

    val dataFolder: Path get() = Path.of("api-data")
}
