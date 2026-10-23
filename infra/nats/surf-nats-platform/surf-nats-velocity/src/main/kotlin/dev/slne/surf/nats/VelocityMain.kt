package dev.slne.surf.nats

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.annotation.DataDirectory
import kotlinx.coroutines.runBlocking
import java.nio.file.Path

@InternalNatsApi
lateinit var plugin: VelocityMain
    private set

@InternalNatsApi
class VelocityMain @Inject constructor(
    @DataDirectory val dataPath: Path
) {
    init {
        plugin = this
        runBlocking {
            NatsInstance.get().load()
        }
    }

    @Subscribe(priority = Short.MIN_VALUE)
    suspend fun onShutdown(@Suppress("unused") event: ProxyShutdownEvent) {
        NatsInstance.instance.disable()
    }
}
