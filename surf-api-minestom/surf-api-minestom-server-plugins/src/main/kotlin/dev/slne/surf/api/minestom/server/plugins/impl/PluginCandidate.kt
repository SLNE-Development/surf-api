package dev.slne.surf.api.minestom.server.plugins.impl

import dev.slne.surf.api.minestom.plugin.PluginMeta
import kotlinx.serialization.json.Json
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import java.nio.file.Path
import java.util.jar.JarFile
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries

/**
 * A plugin that was found but not loaded yet.
 *
 * @property jar the plugin's jar, or `null` for a plugin on the server's own class path
 */
internal data class PluginCandidate(
    val meta: PluginMeta,
    val jar: Path?,
) {
    val source: String get() = jar?.fileName?.toString() ?: "class path"
}

/**
 * Finds the plugins in a directory and on the server's class path.
 */
internal object PluginDiscovery {
    private val LOGGER = ComponentLogger.logger()

    private val json = Json {
        ignoreUnknownKeys = true
    }

    /** Every jar in [directory] that carries a `minestom-plugin.json`. */
    fun fromDirectory(directory: Path): List<PluginCandidate> {
        if (!directory.isDirectory()) return emptyList()

        return directory.listDirectoryEntries()
            .filter { it.isRegularFile() && it.extension.equals("jar", ignoreCase = true) }
            .sortedBy { it.fileName.toString() }
            .mapNotNull(::fromJar)
    }

    fun fromJar(jar: Path): PluginCandidate? = try {
        JarFile(jar.toFile()).use { file ->
            val entry = file.getJarEntry(PluginMeta.FILE_NAME)
            if (entry == null) {
                LOGGER.warn("{} is no plugin, it carries no {}", jar.fileName, PluginMeta.FILE_NAME)
                return null
            }

            val content = file.getInputStream(entry).use { it.readBytes().decodeToString() }
            PluginCandidate(parse(content), jar)
        }
    } catch (failure: Exception) {
        LOGGER.error("Could not read the plugin {}", jar.fileName, failure)
        null
    }

    /**
     * The plugins shaded into the server itself, listed in [PluginMeta.CLASSPATH_INDEX].
     */
    fun fromClassPath(classLoader: ClassLoader): List<PluginCandidate> {
        val mainClasses = classLoader.getResources(PluginMeta.CLASSPATH_INDEX).toList()
            .flatMap { url -> url.openStream().use { it.readBytes().decodeToString() }.lines() }
            .map { line -> line.substringBefore('#').trim() }
            .filter(String::isNotEmpty)
            .distinct()

        return mainClasses.mapNotNull { mainClass ->
            val path = "${PluginMeta.CLASSPATH_DIRECTORY}$mainClass.json"
            val resource = classLoader.getResource(path)
            if (resource == null) {
                LOGGER.warn("The plugin {} on the class path has no {}", mainClass, path)
                return@mapNotNull null
            }

            try {
                val meta = parse(resource.openStream().use { it.readBytes().decodeToString() })
                PluginCandidate(meta, jar = null)
            } catch (failure: Exception) {
                LOGGER.error("Could not read the plugin {} on the class path", mainClass, failure)
                null
            }
        }
    }

    fun parse(content: String): PluginMeta = json.decodeFromString(PluginMeta.serializer(), content)
}
