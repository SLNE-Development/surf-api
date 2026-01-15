package dev.slne.surf.surfapi.hytale.server.impl

import com.google.auto.service.AutoService
import com.hypixel.hytale.server.core.entity.entities.Player
import dev.slne.surf.surfapi.core.api.SurfCoreApi
import dev.slne.surf.surfapi.core.api.util.checkInstantiationByServiceLoader
import dev.slne.surf.surfapi.core.server.impl.SurfCoreApiImpl
import dev.slne.surf.surfapi.hytale.api.SurfHytaleApi
import java.util.*


@AutoService(SurfCoreApi::class)
class SurfHytaleApiImpl : SurfCoreApiImpl(), SurfHytaleApi {
    init {
        checkInstantiationByServiceLoader()
    }

    override fun sendPlayerToServer(playerUuid: UUID, server: String) {
        throw NotImplementedError()
    }

    override fun getPlayer(playerUuid: UUID): Player = throw NotImplementedError()
}
