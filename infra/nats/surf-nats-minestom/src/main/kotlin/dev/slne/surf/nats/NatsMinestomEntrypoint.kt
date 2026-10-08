package dev.slne.surf.nats

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.plugin.MinestomPluginEntrypoint
import dev.slne.minestom.lobby.api.plugin.annotation.DataDirectory
import java.nio.file.Path

@Singleton
@InternalNatsApi
class NatsMinestomEntrypoint @Inject constructor(
    @DataDirectory path: Path,
) : MinestomPluginEntrypoint {

    init {
        dataPath = path
    }

    override suspend fun start() {
        NatsInstance.get().load()
    }

    override suspend fun stop() {
        NatsInstance.get().disable()
    }

    companion object {
        lateinit var dataPath: Path
            private set
    }
}