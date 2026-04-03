package dev.slne.surf.surfapi.standalone.impl

import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.core.api.SurfApiCore
import dev.slne.surf.surfapi.core.api.util.checkInstantiationByServiceLoader
import dev.slne.surf.surfapi.core.server.impl.SurfApiCoreImpl
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
