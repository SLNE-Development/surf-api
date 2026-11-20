package dev.slne.minestom.fixtures.datadir

import dev.slne.surf.api.minestom.plugin.SurfMinestomPlugin
import dev.slne.surf.api.spring.SurfLifecycle
import org.springframework.stereotype.Component
import java.nio.file.Files

/**
 * A plugin that records whether its data directory exists while it is constructed and while a bean
 * of its context loads.
 */
class DataDirectoryPlugin : SurfMinestomPlugin() {
    private val existedOnConstruction = Files.isDirectory(dataDirectory)

    init {
        constructionSawDirectory = existedOnConstruction
    }

    companion object {
        var constructionSawDirectory: Boolean? = null
        var loadSawDirectory: Boolean? = null
    }
}

@Component
class DataDirectoryLifecycle(private val plugin: DataDirectoryPlugin) : SurfLifecycle {
    override suspend fun onLoad() {
        DataDirectoryPlugin.loadSawDirectory = Files.isDirectory(plugin.dataDirectory)
    }
}
