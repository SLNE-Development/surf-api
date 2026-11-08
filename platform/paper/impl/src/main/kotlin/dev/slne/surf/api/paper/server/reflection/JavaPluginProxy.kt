package dev.slne.surf.api.paper.server.reflection

import dev.slne.surf.api.core.reflection.Name
import dev.slne.surf.api.core.reflection.SurfProxy
import org.bukkit.plugin.java.JavaPlugin

@SurfProxy(JavaPlugin::class)
interface JavaPluginProxy {

    @Name("getClassLoader")
    fun getClassLoader(instance: JavaPlugin): ClassLoader
}