package dev.slne.surf.api.core.server.component.property

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.expireAfterAccess
import dev.slne.surf.api.di.condition.ConditionalOnProperty
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.div
import kotlin.io.path.isRegularFile
import kotlin.io.path.notExists
import kotlin.time.Duration.Companion.minutes

/**
 * Service for loading and accessing plugin-specific properties.
 *
 * Properties are loaded from YAML files in the plugin's data folder.
 * By default, properties are loaded from `properties.yml` but custom
 * file paths can be specified.
 *
 * This class supports nested property access using dot notation.
 * For example, `database.host` will look for:
 * ```yaml
 * database:
 *   host: localhost
 * ```
 *
 * **An absent file and an unreadable one are different answers.** No file, or a path that is not a
 * regular file, means the property is not set, and a condition is entitled to fall back to its
 * default. A file that exists and cannot be parsed throws, because the container asks this to
 * decide whether a bean is created at all: treating a broken file as "not set" would start the
 * server with guarded features on whichever side their defaults happen to be, and say nothing.
 */
object PropertyService {

    private val propertyCache = Caffeine.newBuilder()
        .expireAfterAccess(30.minutes)
        .build<PropertyCacheKey, ConfigurationNode>()

    private data class PropertyCacheKey(
        val dataFolder: String,
        val file: String
    )

    /**
     * Gets a property value from the plugin's properties file.
     *
     * @param dataPath The plugin's data path
     * @param key The property key
     * @param file Optional custom file path relative to the data folder
     * @return The property value as a string, or null if not found
     */
    fun getProperty(dataPath: Path, key: Array<String>, file: String = ""): String? {
        val properties = loadProperties(dataPath, file)
        val node = properties.node(*key)
        return node.string
    }

    /**
     * Checks if a property exists in the plugin's properties file.
     *
     * @param dataPath The plugin's data path
     * @param key The property key
     * @param file Optional custom file path relative to the data folder
     * @return true if the property exists, false otherwise
     */
    fun hasProperty(dataPath: Path, key: Array<String>, file: String = ""): Boolean {
        val properties = loadProperties(dataPath, file)
        val node = properties.node(*key)
        return !node.virtual()
    }

    fun clearCache() {
        propertyCache.invalidateAll()
    }

    private fun loadProperties(dataPath: Path, file: String): ConfigurationNode {
        val fileName = file.ifBlank { ConditionalOnProperty.DEFAULT_PROPERTIES_FILE }
        val cacheKey = PropertyCacheKey(
            dataFolder = dataPath.absolutePathString(),
            file = fileName
        )

        return propertyCache.get(cacheKey) { key ->
            val propertiesPath = dataPath / key.file
            if (propertiesPath.notExists() || !propertiesPath.isRegularFile()) {
                return@get CommentedConfigurationNode.root()
            }

            // Deliberately not caught. A file that is there but cannot be read is not the same
            // thing as a property that is not set, and returning an empty node makes them
            // identical -- after which `@ConditionalOnProperty`'s `matchIfMissing` decides, and a
            // server starts with every guarded feature on the wrong side of its default and
            // nothing in the log to say why. The cache stores nothing for a failed load, so fixing
            // the file and retrying works.
            try {
                YamlConfigurationLoader.builder()
                    .path(propertiesPath)
                    .build()
                    .load()
            } catch (cause: Exception) {
                throw IllegalStateException(
                    "Cannot read ${propertiesPath.toAbsolutePath()}. The file is there, so it is " +
                            "not being treated as an unset property: that would let a condition " +
                            "fall back to its default and start the server in a state nobody " +
                            "asked for. Fix the file.",
                    cause,
                )
            }
        }
    }
}