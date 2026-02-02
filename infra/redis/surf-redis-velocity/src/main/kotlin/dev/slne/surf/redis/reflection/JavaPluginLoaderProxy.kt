package dev.slne.surf.redis.reflection

import com.velocitypowered.api.proxy.ProxyServer
import dev.slne.surf.surfapi.core.api.reflection.*
import java.nio.file.Path
import java.util.*

@SurfProxy(qualifiedName = "com.velocitypowered.proxy.plugin.loader.java.JavaPluginLoader")
interface JavaPluginLoaderProxy {

    @Constructor
    fun createInstance(server: ProxyServer, baseDirectory: Path): Any

    @Name("getSerializedPluginInfo")
    fun getSerializedPluginInfo(instance: Any, source: Path): Optional<Any>

    companion object {
        val instance = surfReflection.createProxy<JavaPluginLoaderProxy>()
        fun get() = instance
    }
}