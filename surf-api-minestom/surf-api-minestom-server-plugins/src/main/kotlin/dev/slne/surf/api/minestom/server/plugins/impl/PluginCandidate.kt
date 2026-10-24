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
 * @property jar the plugin's jar, or `null` for a plugin on the system class path
 */
internal data class PluginCandidate(
    val meta: PluginMeta,
    val jar: Path?,
) {
    val source: String get() = jar?.fileName?.toString() ?: "class path"
}

/**
 * Finds the plugins in a directory and on the system class path.
 *
 * A plugin that is found but whose description cannot be read is logged and added to the
 * `failures` of the call, keyed by its jar or main class, with the reason.
 */
internal object PluginDiscovery {
    private val LOGGER = ComponentLogger.logger()

    private val json = Json {
        ignoreUnknownKeys = true
    }

    /**
     * Every jar in [directory] that carries a `minestom-plugin.json`.
     */
    fun fromDirectory(directory: Path, failures: MutableMap<String, String> = mutableMapOf()): List<PluginCandidate> {
        if (!directory.isDirectory()) return emptyList()

        return directory.listDirectoryEntries()
            .filter { it.isRegularFile() && it.extension.equals("jar", ignoreCase = true) }
            .sortedBy { it.fileName.toString() }
            .mapNotNull { jar -> fromJar(jar, failures) }
    }

    fun fromJar(jar: Path, failures: MutableMap<String, String> = mutableMapOf()): PluginCandidate? = try {
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
        failures[jar.fileName.toString()] = "its description could not be read: ${describe(failure)}"
        null
    }

    /**
     * The plugins shaded into the server itself, listed in [PluginMeta.CLASSPATH_INDEX].
     */
    fun fromClassPath(classLoader: ClassLoader, failures: MutableMap<String, String> = mutableMapOf()): List<PluginCandidate> {
        val mainClasses = classLoader.getResources(PluginMeta.CLASSPATH_INDEX).asSequence()
            .flatMap { url -> url.openStream().use { it.readBytes().decodeToString() }.lines() }
            .map { line -> line.substringBefore('#').trim() }
            .filter(String::isNotEmpty)
            .distinct()
            .toList()

        return mainClasses.mapNotNull { mainClass ->
            val path = "${PluginMeta.CLASSPATH_DIRECTORY}$mainClass.json"
            val resource = classLoader.getResource(path)
            if (resource == null) {
                LOGGER.error("The plugin {} on the class path has no {}", mainClass, path)
                failures[mainClass] = "its description $path is missing"
                return@mapNotNull null
            }

            try {
                val meta = parse(resource.openStream().use { it.readBytes().decodeToString() })
                PluginCandidate(meta, jar = null)
            } catch (failure: Exception) {
                LOGGER.error("Could not read the plugin {} on the class path", mainClass, failure)
                failures[mainClass] = "its description could not be read: ${describe(failure)}"
                null
            }
        }
    }

    fun parse(content: String): PluginMeta = json.decodeFromString(PluginMeta.serializer(), content)

    private fun describe(failure: Throwable): String =
        failure.message?.let { "${failure.javaClass.simpleName}: $it" } ?: failure.javaClass.simpleName
}
