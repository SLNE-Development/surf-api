@file:OptIn(InternalSurfApi::class)

package dev.slne.surf.api.velocity

import dev.slne.surf.api.core.SurfApiCore
import dev.slne.surf.api.di.surfBean
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import java.util.concurrent.ExecutorService

private val instance by surfBean<SurfApiVelocity>()

interface SurfApiVelocity : SurfApiCore {
    val executorService: ExecutorService?

    companion object {
        val INSTANCE get() = instance
    }
}
