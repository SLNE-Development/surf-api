package dev.slne.surf.api.standalone.impl

import com.google.auto.service.AutoService
import dev.slne.surf.api.core.SurfApiCore
import dev.slne.surf.api.core.server.impl.SurfApiCoreImpl
import dev.slne.surf.api.core.util.checkInstantiationByServiceLoader
import java.nio.file.Path
import java.util.*

@AutoService(SurfApiCore::class)
class SurfStandaloneApiImpl : SurfApiCoreImpl() {
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
