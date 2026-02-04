package dev.slne.surf.surfapi.core.server.component.property

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.expireAfterAccess
import dev.slne.surf.surfapi.core.api.util.logger
import dev.slne.surf.surfapi.shared.api.component.requirement.ConditionalOnProperty
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
 */
object PropertyService {
    private val log = logger()

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

            try {
                YamlConfigurationLoader.builder()
                    .path(propertiesPath)
                    .build()
                    .load()
            } catch (e: Exception) {
                log.atWarning()
                    .log("Failed to load properties file: %s", propertiesPath.toAbsolutePath(), e)
                CommentedConfigurationNode.root()
            }
        }
    }
}