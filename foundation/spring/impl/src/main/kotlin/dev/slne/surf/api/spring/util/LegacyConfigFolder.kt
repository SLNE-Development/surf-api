package dev.slne.surf.api.spring.util

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import org.apache.commons.logging.LogFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

/**
 * Locates a configuration folder inside surf-api's data folder and carries the configuration file
 * over from the data folder of a retired plugin.
 *
 * [legacyPluginFolders] are names of siblings of surf-api's data folder, searched in order; the
 * first one holding a `config.yml` wins. [label] names the configuration in log messages.
 */
@InternalSurfApi
class LegacyConfigFolder(
    private val folderName: String,
    private val legacyPluginFolders: List<String>,
    private val label: String,
) {

    /**
     * Returns the folder [folderName] in [surfApiDataDirectory], copying the configuration of a
     * legacy plugin folder into it when it holds no configuration yet.
     *
     * The copy is one-time: the legacy file stays untouched, and a failed copy is logged and leaves
     * the defaults to be written.
     */
    fun resolve(surfApiDataDirectory: Path): Path {
        val folder = surfApiDataDirectory.resolve(folderName)
        val target = folder.resolve(CONFIG_FILE_NAME)
        if (!target.exists()) {
            legacyConfig(surfApiDataDirectory)?.let { copy(it, target) }
        }
        return folder
    }

    private fun legacyConfig(surfApiDataDirectory: Path): Path? {
        val pluginsFolder = surfApiDataDirectory.parent ?: return null
        return legacyPluginFolders.asSequence()
            .map { pluginsFolder.resolve(it).resolve(CONFIG_FILE_NAME) }
            .firstOrNull { it.isRegularFile() }
    }

    private fun copy(legacy: Path, target: Path) {
        try {
            Files.createDirectories(target.parent)
            Files.copy(legacy, target)
            log.info("Copied the $label configuration from $legacy to $target")
        } catch (failure: IOException) {
            log.warn("Could not copy the $label configuration from $legacy to $target", failure)
        }
    }

    private companion object {
        private const val CONFIG_FILE_NAME = "config.yml"

        private val log = LogFactory.getLog(LegacyConfigFolder::class.java)
    }
}
