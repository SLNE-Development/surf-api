package dev.slne.surf.api.config

import org.spongepowered.configurate.jackson.JacksonConfigurationLoader
import org.spongepowered.configurate.loader.AbstractConfigurationLoader
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.nio.file.Path
import kotlin.io.path.extension

/**
 * The file format of a configuration file.
 */
enum class ConfigFormat {

    /**
     * YAML in block style, with the `@Comment` texts of the config class written as comments.
     */
    YAML {
        override fun loaderBuilder(): AbstractConfigurationLoader.Builder<*, *> {
            return YamlConfigurationLoader.builder()
                .nodeStyle(NodeStyle.BLOCK)
                .commentsEnabled(true)
        }
    },

    /**
     * JSON; comments are dropped.
     */
    JSON {
        override fun loaderBuilder(): AbstractConfigurationLoader.Builder<*, *> {
            return JacksonConfigurationLoader.builder()
        }
    },
    ;

    internal abstract fun loaderBuilder(): AbstractConfigurationLoader.Builder<*, *>

    companion object {

        /**
         * Returns the format the extension of [path] names, case-insensitively.
         *
         * @param path the configuration file
         * @return [YAML] for `.yml` and `.yaml`, [JSON] for `.json`, `null` for any other extension
         */
        fun ofPath(path: Path): ConfigFormat? {
            return when (path.extension.lowercase()) {
                "yml", "yaml" -> YAML
                "json" -> JSON
                else -> null
            }
        }
    }
}
