package dev.slne.surf.surfapi.bukkit.server.reflection

import dev.slne.surf.surfapi.core.api.reflection.Name
import dev.slne.surf.surfapi.core.api.reflection.SurfProxy
import org.bukkit.plugin.java.JavaPlugin

@SurfProxy(JavaPlugin::class)
interface JavaPluginProxy {

    @Name("getClassLoader")
    fun getClassLoader(instance: JavaPlugin): ClassLoader
}