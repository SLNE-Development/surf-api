package dev.slne.surf.nats

import dev.slne.surf.api.minestom.plugin.SurfMinestomPlugin

@InternalNatsApi
object NatsMinestomPlugin : SurfMinestomPlugin() {

    override suspend fun onLoad() {
        NatsInstance.get().load()
    }

    override suspend fun onDisable() {
        NatsInstance.get().disable()
    }
}
