package dev.slne.surf.api.paper.server.nms.v26_1.reflection

import dev.slne.surf.api.core.reflection.SurfReflection
import dev.slne.surf.api.core.reflection.createProxy
import dev.slne.surf.api.paper.util.reflectionProxy
import xyz.jpenilla.reflectionremapper.ReflectionRemapper
import xyz.jpenilla.reflectionremapper.proxy.ReflectionProxyFactory

object V26_1Reflection {
    lateinit var SERVER_STATS_COUNTER_PROXY: V26_1ServerStatsCounterProxy
        private set
    lateinit var ENTITY_PROXY: V26_1EntityProxy
        private set
    lateinit var ITEM_PROXY: V26_1ItemProxy
        private set
    lateinit var SERVER_CONNECTION_LISTENER_PROXY: V26_1ServerConnectionListenerProxy
        private set
    lateinit var VANILLA_ARGUMENT_PROVIDER_IMPL_PROXY: V26_1VanillaArgumentProviderImplProxy
        private set
    lateinit var VANILLA_ARGUMENT_PROVIDER_PROXY: V26_1VanillaArgumentProviderProxy
        private set

    fun initialize() {
        val remapper = ReflectionRemapper.forReobfMappingsInPaperJar()
        val proxyFactory =
            ReflectionProxyFactory.create(remapper, V26_1Reflection::class.java.classLoader)

        SERVER_STATS_COUNTER_PROXY = proxyFactory.reflectionProxy<V26_1ServerStatsCounterProxy>()
        ENTITY_PROXY = proxyFactory.reflectionProxy<V26_1EntityProxy>()
        ITEM_PROXY = proxyFactory.reflectionProxy<V26_1ItemProxy>()
        SERVER_CONNECTION_LISTENER_PROXY =
            proxyFactory.reflectionProxy<V26_1ServerConnectionListenerProxy>()
        VANILLA_ARGUMENT_PROVIDER_IMPL_PROXY =
            SurfReflection.createProxy<V26_1VanillaArgumentProviderImplProxy>()
        VANILLA_ARGUMENT_PROVIDER_PROXY =
            SurfReflection.createProxy<V26_1VanillaArgumentProviderProxy>()

        // gc the remapper
        System.gc()
    }
}
