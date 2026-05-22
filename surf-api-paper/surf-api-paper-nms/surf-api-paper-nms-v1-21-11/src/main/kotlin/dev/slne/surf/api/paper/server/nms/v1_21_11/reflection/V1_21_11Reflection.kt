package dev.slne.surf.api.paper.server.nms.v1_21_11.reflection

import dev.slne.surf.api.paper.util.reflectionProxy
import xyz.jpenilla.reflectionremapper.ReflectionRemapper
import xyz.jpenilla.reflectionremapper.proxy.ReflectionProxyFactory

object V1_21_11Reflection {
    lateinit var ITEM_PROXY: V1_21_11ItemProxy
        private set

    fun initialize() {
        val remapper = ReflectionRemapper.forReobfMappingsInPaperJar()
        val proxyFactory =
            ReflectionProxyFactory.create(remapper, V1_21_11Reflection::class.java.classLoader)

        ITEM_PROXY = proxyFactory.reflectionProxy<V1_21_11ItemProxy>()

        // gc the remapper
        System.gc()
    }
}
