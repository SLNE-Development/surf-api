package dev.slne.surf.api.velocity

import dev.slne.surf.api.core.SurfApiCore
import dev.slne.surf.api.spring.SurfApiBean
import java.util.concurrent.ExecutorService

@SurfApiBean
interface SurfApiVelocity : SurfApiCore {
    val executorService: ExecutorService?
}
