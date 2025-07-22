package dev.slne.surf.surfapi.bukkit.server.reflection

import dev.slne.surf.surfapi.bukkit.api.util.reflectionProxy
import dev.slne.surf.surfapi.core.api.reflection.createProxy
import dev.slne.surf.surfapi.core.api.reflection.surfReflection
import xyz.jpenilla.reflectionremapper.ReflectionRemapper
import xyz.jpenilla.reflectionremapper.proxy.ReflectionProxyFactory

object Reflection {
    val SERVER_STATS_COUNTER_PROXY: ServerStatsCounterProxy
    val ITEM_PROXY: ItemProxy
    val ENTITY_PROXY: EntityProxy
    val SERVER_CONNECTION_LISTENER_PROXY: ServerConnectionListenerProxy

    init {
        val remapper = ReflectionRemapper.forReobfMappingsInPaperJar()
        val proxyFactory =
            ReflectionProxyFactory.create(remapper, Reflection::class.java.getClassLoader())

        SERVER_STATS_COUNTER_PROXY = proxyFactory.reflectionProxy<ServerStatsCounterProxy>()
        ITEM_PROXY = surfReflection.createProxy<ItemProxy>()
        ENTITY_PROXY = proxyFactory.reflectionProxy<EntityProxy>()
        SERVER_CONNECTION_LISTENER_PROXY = proxyFactory.reflectionProxy<ServerConnectionListenerProxy>()

        // gc the remapper
        System.gc()
    }
}
