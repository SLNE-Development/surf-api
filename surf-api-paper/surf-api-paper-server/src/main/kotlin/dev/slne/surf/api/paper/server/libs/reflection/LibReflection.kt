package dev.slne.surf.api.paper.server.libs.reflection

import dev.slne.surf.api.core.reflection.SurfReflection
import dev.slne.surf.api.core.reflection.createProxy

object LibReflection {
    @JvmField
    val PAPER_PLUGIN_CLASS_LOADER_PROXY = SurfReflection.createProxy<PaperPluginClassLoaderProxy>()
}
