package dev.slne.surf.surfapi.core.server.component.property

import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import java.time.Duration

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
    private val logger = LoggerFactory.getLogger(PropertyService::class.java)
    
    private const val DEFAULT_PROPERTIES_FILE = "properties.yml"

    private val propertyCache = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(30))
        .build<PropertyCacheKey, Map<String, Any?>>()

    private data class PropertyCacheKey(
        val dataFolder: String,
        val file: String
    )

    /**
     * Gets a property value from the plugin's properties file.
     *
     * @param dataFolder The plugin's data folder
     * @param key The property key (supports dot notation for nested properties)
     * @param file Optional custom file path relative to the data folder
     * @return The property value as a string, or null if not found
     */
    fun getProperty(dataFolder: File, key: String, file: String = ""): String? {
        val properties = loadProperties(dataFolder, file)
        return getNestedProperty(properties, key)?.toString()
    }

    /**
     * Checks if a property exists in the plugin's properties file.
     *
     * @param dataFolder The plugin's data folder
     * @param key The property key (supports dot notation for nested properties)
     * @param file Optional custom file path relative to the data folder
     * @return true if the property exists, false otherwise
     */
    fun hasProperty(dataFolder: File, key: String, file: String = ""): Boolean {
        val properties = loadProperties(dataFolder, file)
        return getNestedProperty(properties, key) != null
    }

    /**
     * Clears the property cache, forcing a reload on next access.
     * This is primarily useful for testing purposes.
     */
    fun clearCache() {
        propertyCache.invalidateAll()
    }

    /**
     * Clears the cache for a specific plugin's properties.
     *
     * @param dataFolder The plugin's data folder
     * @param file Optional custom file path
     */
    fun clearCache(dataFolder: File, file: String = "") {
        val cacheKey = PropertyCacheKey(
            dataFolder = dataFolder.absolutePath,
            file = file.ifBlank { DEFAULT_PROPERTIES_FILE }
        )
        propertyCache.invalidate(cacheKey)
    }

    private fun loadProperties(dataFolder: File, file: String): Map<String, Any?> {
        val fileName = file.ifBlank { DEFAULT_PROPERTIES_FILE }
        val cacheKey = PropertyCacheKey(
            dataFolder = dataFolder.absolutePath,
            file = fileName
        )

        return propertyCache.get(cacheKey) { key ->
            val propertiesFile = File(dataFolder, key.file)
            if (!propertiesFile.exists() || !propertiesFile.isFile) {
                return@get emptyMap()
            }

            try {
                val loader = YamlConfigurationLoader.builder()
                    .file(propertiesFile)
                    .build()
                val rootNode = loader.load()
                nodeToMap(rootNode)
            } catch (e: Exception) {
                logger.debug("Failed to load properties file: {}", propertiesFile.absolutePath, e)
                emptyMap()
            }
        }
    }

    private fun nodeToMap(node: org.spongepowered.configurate.ConfigurationNode): Map<String, Any?> {
        if (node.isMap) {
            val result = mutableMapOf<String, Any?>()
            for ((key, value) in node.childrenMap()) {
                val keyStr = key.toString()
                result[keyStr] = if (value.isMap) {
                    nodeToMap(value)
                } else {
                    value.raw()
                }
            }
            return result
        }
        return emptyMap()
    }

    @Suppress("UNCHECKED_CAST")
    private fun getNestedProperty(properties: Map<String, Any?>, key: String): Any? {
        val parts = key.split(".")
        var current: Any? = properties

        for (part in parts) {
            current = when (current) {
                is Map<*, *> -> (current as Map<String, Any?>)[part]
                else -> return null
            }
            if (current == null) return null
        }

        return current
    }
}
