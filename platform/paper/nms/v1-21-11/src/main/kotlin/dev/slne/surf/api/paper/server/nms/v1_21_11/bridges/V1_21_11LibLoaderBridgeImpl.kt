package dev.slne.surf.api.paper.server.nms.v1_21_11.bridges

import dev.slne.surf.api.core.reflection.Field
import dev.slne.surf.api.core.reflection.Field.Type
import dev.slne.surf.api.core.reflection.SurfProxy
import dev.slne.surf.api.core.reflection.SurfReflection
import dev.slne.surf.api.core.reflection.createProxy
import dev.slne.surf.api.core.util.setFinalField
import dev.slne.surf.api.paper.nms.common.LibLoaderBridge
import dev.slne.surf.api.paper.server.nms.v1_21_11.extensions.commodore
import dev.slne.surf.api.paper.server.nms.v1_21_11.extensions.craftServer
import io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader
import org.bukkit.craftbukkit.util.ApiVersion
import java.net.URLClassLoader

@Suppress("ClassName")
object V1_21_11LibLoaderBridgeImpl : LibLoaderBridge {

    override fun getActiveCompatibilities(): MutableSet<String> {
        return craftServer.activeCompatibilities
    }

    override fun convertWithCommodore(
        b: ByteArray,
        pluginName: String,
        pluginVersion: String,
        activeCompatibilities: MutableSet<String>
    ): ByteArray {
        return commodore.convert(
            b,
            pluginName,
            ApiVersion.getOrCreateVersion(pluginVersion),
            activeCompatibilities
        )
    }

    @Suppress("DEPRECATION")
    override fun overwriteLibraryLoader(pluginClassLoader: URLClassLoader, newLoader: URLClassLoader) {
        PaperPluginClassLoader::class.java.getDeclaredField("libraryLoader").apply {
            setAccessible(true)
            setFinalField(this, pluginClassLoader, newLoader)
        }
    }

    override fun getLibraryLoader(pluginClassLoader: URLClassLoader): URLClassLoader {
        return PaperPluginClassLoaderProxy.instance.getLibraryLoader(pluginClassLoader as PaperPluginClassLoader)
    }

    @SurfProxy(PaperPluginClassLoader::class)
    interface PaperPluginClassLoaderProxy {
        @Field("libraryLoader", Type.GETTER)
        fun getLibraryLoader(instance: PaperPluginClassLoader): URLClassLoader

        companion object {
            val instance = SurfReflection.createProxy<PaperPluginClassLoaderProxy>()
        }
    }
}