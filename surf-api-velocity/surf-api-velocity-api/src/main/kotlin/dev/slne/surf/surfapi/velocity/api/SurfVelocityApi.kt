package dev.slne.surf.surfapi.velocity.api

import dev.slne.surf.surfapi.core.api.SurfCoreApi
import dev.slne.surf.surfapi.core.api.util.requiredService
import java.util.concurrent.ExecutorService

interface SurfVelocityApi : SurfCoreApi {
    val executorService: ExecutorService?

    companion object {
        val instance = requiredService<SurfVelocityApi>()
    }
}

val surfVelocityApi get() = SurfVelocityApi.instance