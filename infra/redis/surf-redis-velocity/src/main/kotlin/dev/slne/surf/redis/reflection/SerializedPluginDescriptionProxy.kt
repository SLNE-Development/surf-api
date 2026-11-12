package dev.slne.surf.redis.reflection

import dev.slne.surf.api.reflection.Name
import dev.slne.surf.api.reflection.SurfProxy
import dev.slne.surf.api.reflection.SurfReflection
import dev.slne.surf.api.reflection.createProxy

@SurfProxy(qualifiedName = "com.velocitypowered.api.plugin.ap.SerializedPluginDescription")
interface SerializedPluginDescriptionProxy {

    @Name("getId")
    fun getId(instance: Any): String

    companion object : SerializedPluginDescriptionProxy by proxy {
        val INSTANCE get() = proxy
    }
}

private val proxy = SurfReflection.INSTANCE.createProxy<SerializedPluginDescriptionProxy>()