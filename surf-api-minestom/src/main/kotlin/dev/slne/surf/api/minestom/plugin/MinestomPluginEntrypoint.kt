package dev.slne.surf.api.minestom.plugin

interface MinestomPluginEntrypoint {
    suspend fun start()

    suspend fun afterStart() = Unit

    suspend fun stop() = Unit
}
