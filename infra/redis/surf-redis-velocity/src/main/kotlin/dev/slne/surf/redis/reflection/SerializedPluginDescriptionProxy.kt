package dev.slne.surf.redis.reflection

import dev.slne.surf.api.core.reflection.Name
import dev.slne.surf.api.core.reflection.SurfProxy
import dev.slne.surf.api.core.reflection.SurfReflection
import dev.slne.surf.api.core.reflection.createProxy

@SurfProxy(qualifiedName = "com.velocitypowered.api.plugin.ap.SerializedPluginDescription")
interface SerializedPluginDescriptionProxy {

    @Name("getId")
    fun getId(instance: Any): String

    companion object : SerializedPluginDescriptionProxy by proxy {
        val INSTANCE get() = proxy
    }
}

private val proxy = SurfReflection.createProxy<SerializedPluginDescriptionProxy>()