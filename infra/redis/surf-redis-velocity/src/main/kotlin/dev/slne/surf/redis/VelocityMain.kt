package dev.slne.surf.redis

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.annotation.DataDirectory
import java.nio.file.Path

class VelocityMain @Inject constructor(@param:DataDirectory val dataPath: Path) {
    init {
        plugin = this
        RedisInstance.instance.load()
    }

    @Subscribe(priority = Short.MIN_VALUE)
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        RedisInstance.instance.disable()
    }

}

lateinit var plugin: VelocityMain