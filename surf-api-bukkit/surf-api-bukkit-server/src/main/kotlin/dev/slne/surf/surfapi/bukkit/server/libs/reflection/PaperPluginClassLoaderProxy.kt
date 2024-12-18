package dev.slne.surf.surfapi.bukkit.server.libs.reflection

import dev.slne.surf.surfapi.core.api.reflection.Field
import dev.slne.surf.surfapi.core.api.reflection.Field.Type
import dev.slne.surf.surfapi.core.api.reflection.SurfProxy
import io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader
import java.net.URLClassLoader

@SurfProxy(PaperPluginClassLoader::class)
interface PaperPluginClassLoaderProxy {
    @Field("libraryLoader", Type.SETTER, overrideFinal = true)
    fun setLibraryLoader(instance: PaperPluginClassLoader, libraryLoader: URLClassLoader)

    @Field("libraryLoader", Type.GETTER)
    fun getLibraryLoader(instance: PaperPluginClassLoader): URLClassLoader
}
