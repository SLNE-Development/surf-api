package dev.slne.surf.api.minestom.server.plugins

import dev.slne.surf.api.minestom.plugin.SurfMinestomPlugin
import java.nio.file.Files

/**
 * A plugin that records its lifecycle calls and never fails.
 */
class RecordingPlugin : SurfMinestomPlugin() {
    override suspend fun onLoad() {
        events += "load"
    }

    override suspend fun onEnable() {
        events += "enable"
    }

    override suspend fun onDisable() {
        events += "disable"
    }

    companion object {
        val events = mutableListOf<String>()
    }
}

/**
 * A plugin whose `onLoad` throws.
 */
class FailingLoadPlugin : SurfMinestomPlugin() {
    override suspend fun onLoad() {
        error("onLoad fails on purpose")
    }
}

/**
 * A plugin whose `onEnable` throws.
 */
class FailingEnablePlugin : SurfMinestomPlugin() {
    override suspend fun onEnable() {
        error("onEnable fails on purpose")
    }
}

/**
 * A plugin that records whether its data directory exists while it is constructed and loaded.
 */
class DataDirectoryPlugin : SurfMinestomPlugin() {
    private val existedOnConstruction = Files.isDirectory(dataDirectory)

    override suspend fun onLoad() {
        constructionSawDirectory = existedOnConstruction
        loadSawDirectory = Files.isDirectory(dataDirectory)
    }

    companion object {
        var constructionSawDirectory: Boolean? = null
        var loadSawDirectory: Boolean? = null
    }
}
