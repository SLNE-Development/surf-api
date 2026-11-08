package dev.slne.surf.api.velocity.server.reflection

import dev.slne.surf.api.core.reflection.SurfReflection
import dev.slne.surf.api.core.reflection.createProxy

object VelocityReflection {
    val EVENT_MANAGER_PROXY = SurfReflection.createProxy<EventManagerProxy>()
}