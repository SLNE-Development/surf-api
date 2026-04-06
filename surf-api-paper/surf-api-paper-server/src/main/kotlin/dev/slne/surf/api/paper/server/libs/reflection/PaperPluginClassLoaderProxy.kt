package dev.slne.surf.api.paper.server.libs.reflection

import dev.slne.surf.api.core.reflection.Field
import dev.slne.surf.api.core.reflection.Field.Type
import dev.slne.surf.api.core.reflection.SurfProxy
import io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader
import java.net.URLClassLoader

@SurfProxy(PaperPluginClassLoader::class)
interface PaperPluginClassLoaderProxy {
    @Field("libraryLoader", Type.SETTER, overrideFinal = true)
    fun setLibraryLoader(instance: PaperPluginClassLoader, libraryLoader: URLClassLoader)

    @Field("libraryLoader", Type.GETTER)
    fun getLibraryLoader(instance: PaperPluginClassLoader): URLClassLoader
}
