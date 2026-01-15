package dev.slne.surf.surfapi.hytale.api

import dev.slne.surf.surfapi.core.api.SurfCoreApi
import dev.slne.surf.surfapi.core.api.util.requiredService
import java.util.concurrent.ExecutorService

interface SurfHytaleApi : SurfCoreApi {
    val executorService: ExecutorService?

    companion object {
        val instance = requiredService<SurfHytaleApi>()
    }
}

val surfHytaleApi get() = SurfHytaleApi.instance