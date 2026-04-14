package dev.slne.surf.api.paper.server.nms.v1_21_11.reflection

import dev.slne.surf.api.core.reflection.SurfReflection
import dev.slne.surf.api.core.reflection.createProxy
import dev.slne.surf.api.paper.util.reflectionProxy
import xyz.jpenilla.reflectionremapper.ReflectionRemapper
import xyz.jpenilla.reflectionremapper.proxy.ReflectionProxyFactory

object V1_21_11Reflection {
    lateinit var SERVER_STATS_COUNTER_PROXY: V1_21_11ServerStatsCounterProxy
        private set
    lateinit var ENTITY_PROXY: V1_21_11EntityProxy
        private set
    lateinit var SERVER_CONNECTION_LISTENER_PROXY: V1_21_11ServerConnectionListenerProxy
        private set
    lateinit var VANILLA_ARGUMENT_PROVIDER_IMPL_PROXY: V1_21_11VanillaArgumentProviderImplProxy
        private set
    lateinit var VANILLA_ARGUMENT_PROVIDER_PROXY: V1_21_11VanillaArgumentProviderProxy
        private set

    fun initialize() {
        val remapper = ReflectionRemapper.forReobfMappingsInPaperJar()
        val proxyFactory =
            ReflectionProxyFactory.create(remapper, V1_21_11Reflection::class.java.classLoader)

        SERVER_STATS_COUNTER_PROXY = proxyFactory.reflectionProxy<V1_21_11ServerStatsCounterProxy>()
        ENTITY_PROXY = proxyFactory.reflectionProxy<V1_21_11EntityProxy>()
        SERVER_CONNECTION_LISTENER_PROXY =
            proxyFactory.reflectionProxy<V1_21_11ServerConnectionListenerProxy>()
        VANILLA_ARGUMENT_PROVIDER_IMPL_PROXY =
            SurfReflection.createProxy<V1_21_11VanillaArgumentProviderImplProxy>()
        VANILLA_ARGUMENT_PROVIDER_PROXY = SurfReflection.createProxy<V1_21_11VanillaArgumentProviderProxy>()

        // gc the remapper
        System.gc()
    }
}
