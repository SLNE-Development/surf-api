package dev.slne.surf.surfapi.velocity.server.reflection

import dev.slne.surf.surfapi.core.api.reflection.createProxy
import dev.slne.surf.surfapi.core.api.reflection.surfReflection as reflection

object VelocityReflection {
    val EVENT_MANAGER_PROXY = reflection.createProxy<EventManagerProxy>()
}