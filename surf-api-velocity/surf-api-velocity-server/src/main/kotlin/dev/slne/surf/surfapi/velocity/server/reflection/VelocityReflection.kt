package dev.slne.surf.surfapi.velocity.server.reflection

import dev.slne.surf.surfapi.core.api.SurfCoreApi

object VelocityReflection {
    val EVENT_MANAGER_PROXY: EventManagerProxy

    init {
        val reflection = SurfCoreApi.getCore().reflection
        EVENT_MANAGER_PROXY = reflection.createProxy(EventManagerProxy::class.java)
    }
}