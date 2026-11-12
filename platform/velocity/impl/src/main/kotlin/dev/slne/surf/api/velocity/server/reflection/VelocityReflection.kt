package dev.slne.surf.api.velocity.server.reflection

import dev.slne.surf.api.reflection.SurfReflection
import dev.slne.surf.api.reflection.createProxy

object VelocityReflection {
    val EVENT_MANAGER_PROXY = SurfReflection.INSTANCE.createProxy<EventManagerProxy>()
}