package dev.slne.surf.api.paper.server.reflection

import dev.slne.surf.api.core.reflection.SurfReflection
import dev.slne.surf.api.core.reflection.createProxy
import dev.slne.surf.api.paper.util.reflectionProxy
import xyz.jpenilla.reflectionremapper.ReflectionRemapper
import xyz.jpenilla.reflectionremapper.proxy.ReflectionProxyFactory

object Reflection {
    val SERVER_STATS_COUNTER_PROXY: ServerStatsCounterProxy
    val ITEM_PROXY: ItemProxy
    val ENTITY_PROXY: EntityProxy
    val SERVER_CONNECTION_LISTENER_PROXY: ServerConnectionListenerProxy
    val JAVA_PLUGIN_PROXY: JavaPluginProxy
    val VANILLA_ARGUMENT_PROVIDER_IMPL_PROXY: VanillaArgumentProviderImplProxy
    val VANILLA_ARGUMENT_PROVIDER_PROXY: VanillaArgumentProviderProxy

    init {
        val remapper = ReflectionRemapper.forReobfMappingsInPaperJar()
        val proxyFactory =
            ReflectionProxyFactory.create(remapper, Reflection::class.java.classLoader)

        SERVER_STATS_COUNTER_PROXY = proxyFactory.reflectionProxy<ServerStatsCounterProxy>()
        ITEM_PROXY = SurfReflection.createProxy<ItemProxy>()
        ENTITY_PROXY = proxyFactory.reflectionProxy<EntityProxy>()
        SERVER_CONNECTION_LISTENER_PROXY =
            proxyFactory.reflectionProxy<ServerConnectionListenerProxy>()
        JAVA_PLUGIN_PROXY = SurfReflection.createProxy<JavaPluginProxy>()
        VANILLA_ARGUMENT_PROVIDER_IMPL_PROXY =
            SurfReflection.createProxy<VanillaArgumentProviderImplProxy>()
        VANILLA_ARGUMENT_PROVIDER_PROXY = SurfReflection.createProxy<VanillaArgumentProviderProxy>()

        // gc the remapper
        System.gc()
    }
}
