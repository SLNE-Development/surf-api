package dev.slne.surf.api.redis

import dev.slne.surf.api.spring.util.LegacyConfigFolder
import java.nio.file.Path

/**
 * Locates the Redis configuration folder inside surf-api's data folder and carries a configuration
 * over from the folder of the retired `surf-redis-<platform>` plugins (Paper, Velocity and Minestom).
 */
internal object RedisConfigLocation {

    /**
     * The legacy folders are the data folders of the plugins that owned the configuration before
     * the bundling, with and without the `surf-api-` prefix of the module rename.
     */
    private val folder = LegacyConfigFolder(
        folderName = "redis",
        legacyPluginFolders = listOf(
            "surf-api-redis-paper",
            "surf-redis-paper",
            "surf-api-redis-velocity",
            "surf-redis-velocity",
            "surf-api-redis-minestom",
            "surf-redis-minestom",
        ),
        label = "Redis",
    )

    /**
     * Returns the folder that holds the `config.yml` of [dev.slne.surf.redis.config.RedisConfig],
     * copying the file of a legacy plugin folder into it when it holds none yet. See
     * [LegacyConfigFolder.resolve].
     */
    fun resolve(surfApiDataDirectory: Path): Path = folder.resolve(surfApiDataDirectory)
}
