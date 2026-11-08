package dev.slne.surf.api.velocity

import dev.slne.surf.api.core.SurfApiCore
import dev.slne.surf.api.core.util.requiredService
import java.util.concurrent.ExecutorService

private val instance = requiredService<SurfApiVelocity>()

interface SurfApiVelocity : SurfApiCore {
    val executorService: ExecutorService?

    companion object : SurfApiVelocity by instance {
        val INSTANCE get() = instance
    }
}