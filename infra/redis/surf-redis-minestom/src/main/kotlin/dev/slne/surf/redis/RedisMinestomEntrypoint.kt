package dev.slne.surf.redis

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.plugin.MinestomPluginEntrypoint
import dev.slne.minestom.lobby.api.plugin.annotation.DataDirectory
import java.nio.file.Path

@Singleton
class RedisMinestomEntrypoint @Inject constructor(
    @DataDirectory path: Path
): MinestomPluginEntrypoint {

    init {
        dataPath = path
    }

    override suspend fun start() {

    }

    override suspend fun stop() {

    }

    companion object {
        lateinit var dataPath: Path
    }
}