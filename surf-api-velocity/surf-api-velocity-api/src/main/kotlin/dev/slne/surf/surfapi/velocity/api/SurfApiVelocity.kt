package dev.slne.surf.surfapi.velocity.api

import dev.slne.surf.surfapi.core.api.SurfApiCore
import dev.slne.surf.surfapi.core.api.util.requiredService
import java.util.concurrent.ExecutorService

private val instance = requiredService<SurfApiVelocity>()

interface SurfApiVelocity : SurfApiCore {
    val executorService: ExecutorService?

    companion object : SurfApiVelocity by instance {
        val INSTANCE get() = instance
    }
}