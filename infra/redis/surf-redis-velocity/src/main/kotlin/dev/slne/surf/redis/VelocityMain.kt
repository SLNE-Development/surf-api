package dev.slne.surf.redis

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import java.nio.file.Path

class VelocityMain @Inject constructor(@param:DataDirectory val dataPath: Path, val proxy: ProxyServer) {
    init {
        plugin = this
        RedisInstance.instance.load()
    }

    @Subscribe(priority = Short.MIN_VALUE)
    fun onProxyShutdown(@Suppress("unused") event: ProxyShutdownEvent) {
        RedisInstance.instance.disable()
    }

}

lateinit var plugin: VelocityMain