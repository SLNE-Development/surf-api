package dev.slne.surf.nats

import dev.slne.surf.api.minestom.plugin.SurfMinestomPlugin

/**
 * Connects NATS when the Minestom plugin loads and disconnects it when the plugin is disabled.
 */
@InternalNatsApi
object NatsMinestomPlugin : SurfMinestomPlugin() {

    override suspend fun onLoad() {
        NatsInstance.get().load()
    }

    override suspend fun onDisable() {
        NatsInstance.get().disable()
    }
}
