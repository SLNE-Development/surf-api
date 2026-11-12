package dev.slne.surf.nats

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.annotation.DataDirectory
import kotlinx.coroutines.runBlocking
import java.nio.file.Path

/**
 * The running Velocity plugin, set when Velocity constructs it.
 */
@InternalNatsApi
lateinit var plugin: VelocityMain
    private set

/**
 * Connects NATS when Velocity constructs the plugin and disconnects on proxy shutdown.
 *
 * The constructor blocks until the connection is established. The shutdown listener has the lowest priority, so it
 * runs after the shutdown listeners of other plugins.
 */
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
