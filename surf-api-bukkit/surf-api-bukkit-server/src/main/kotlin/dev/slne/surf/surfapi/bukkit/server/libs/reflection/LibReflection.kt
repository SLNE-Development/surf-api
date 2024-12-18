package dev.slne.surf.surfapi.bukkit.server.libs.reflection

import dev.slne.surf.surfapi.core.api.reflection.createProxy
import dev.slne.surf.surfapi.core.api.reflection.surfReflection

object LibReflection {
    @JvmField
    val PAPER_PLUGIN_CLASS_LOADER_PROXY = surfReflection.createProxy<PaperPluginClassLoaderProxy>()
}
