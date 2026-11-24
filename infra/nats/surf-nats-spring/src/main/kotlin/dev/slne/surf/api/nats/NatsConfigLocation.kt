package dev.slne.surf.api.nats

import dev.slne.surf.api.spring.util.LegacyConfigFolder
import java.nio.file.Path

/**
 * Locates the NATS configuration folder inside surf-api's data folder and carries a configuration
 * over from the folder of the retired `surf-nats-<platform>` plugins.
 */
internal object NatsConfigLocation {

    /**
     * The legacy folders are the data folders of the plugins that owned the configuration before
     * the bundling, with and without the `surf-api-` prefix of the module rename.
     */
    private val folder = LegacyConfigFolder(
        folderName = "nats",
        legacyPluginFolders = listOf(
            "surf-api-nats-paper",
            "surf-nats-paper",
            "surf-api-nats-velocity",
            "surf-nats-velocity",
            "surf-api-nats-minestom",
            "surf-nats-minestom",
        ),
        label = "NATS",
    )

    /**
     * Returns the folder that holds the NATS configuration, copying the `config.yml` of a legacy
     * plugin folder into it when it holds no configuration yet. See [LegacyConfigFolder.resolve].
     */
    fun resolve(surfApiDataDirectory: Path): Path = folder.resolve(surfApiDataDirectory)
}
