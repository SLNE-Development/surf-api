package dev.slne.surf.api.paper.server.reflection

import dev.slne.surf.api.core.reflection.SurfReflection
import dev.slne.surf.api.core.reflection.createProxy

object Reflection {
    val JAVA_PLUGIN_PROXY: JavaPluginProxy = SurfReflection.createProxy<JavaPluginProxy>()
}
