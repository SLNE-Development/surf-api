package dev.slne.surf.redis.reflection

import dev.slne.surf.surfapi.core.api.reflection.Name
import dev.slne.surf.surfapi.core.api.reflection.SurfProxy
import dev.slne.surf.surfapi.core.api.reflection.createProxy
import dev.slne.surf.surfapi.core.api.reflection.surfReflection

@SurfProxy(qualifiedName = "com.velocitypowered.api.plugin.ap.SerializedPluginDescription")
interface SerializedPluginDescriptionProxy {

    @Name("getId")
    fun getId(instance: Any): String

    companion object {
        val instance = surfReflection.createProxy<SerializedPluginDescriptionProxy>()
        fun get() = instance
    }
}