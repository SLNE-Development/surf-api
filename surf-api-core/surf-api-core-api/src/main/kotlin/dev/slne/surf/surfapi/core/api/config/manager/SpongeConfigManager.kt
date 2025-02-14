package dev.slne.surf.surfapi.core.api.config.manager

import dev.slne.surf.surfapi.core.api.config.JsonConfigFileNamePattern
import dev.slne.surf.surfapi.core.api.config.YamlConfigFileNamePattern
import dev.slne.surf.surfapi.core.api.config.serializer.SpongeConfigSerializers
import dev.slne.surf.surfapi.core.api.util.logger
import org.jetbrains.annotations.Contract
import org.spongepowered.configurate.ConfigurateException
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.ScopedConfigurationNode
import org.spongepowered.configurate.jackson.JacksonConfigurationLoader
import org.spongepowered.configurate.loader.AbstractConfigurationLoader
import org.spongepowered.configurate.loader.ConfigurationLoader
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.Serial
import java.nio.file.Path

/**
 * Manages configurations using Sponge's Configurate library, including loading, saving, and reloading configurations.
 * Supports multiple formats, including YAML and JSON.
 *
 * @param C The type of the configuration class.
 * @property config The current configuration instance.
 */
class SpongeConfigManager<C> @Contract(pure = true) private constructor(
    private val configClass: Class<C>,
    @JvmField @field:Volatile var config: C,
    private val loader: ConfigurationLoader<out ConfigurationNode>,
    private val node: ConfigurationNode
) {

    /**
     * Saves the current configuration to the file.
     *
     * @throws RuntimeException if an I/O error or serialization error occurs.
     */
    fun save() {
        try {
            node.set(configClass, config)
            loader.save(node)
        } catch (e: ConfigurateException) {
            log.atSevere()
                .withCause(e)
                .log("Failed to save config")
            throw RuntimeException(e)
        }
    }

    /**
     * Reloads the configuration from the file. If loading fails, the current configuration remains unchanged.
     *
     * @return The reloaded configuration instance.
     * @throws RuntimeException if a critical error occurs during reload.
     */
    fun reloadFromFile(): C {
        try {
            val reloadedNode: ConfigurationNode = loader.load()
            val reloadedConfig = reloadedNode.get(configClass)

            if (reloadedConfig == null) {
                log.atWarning()
                    .log("Config is null after reload, using current config")
                return config
            }

            node.set(configClass, reloadedConfig)
            loader.save(node)
            config = reloadedConfig

            return config
        } catch (e: ConfigurateException) {
            log.atSevere()
                .withCause(e)
                .log("Failed to reload config")
            throw RuntimeException(e)
        }
    }



    companion object {
        private val log = logger()

        /**
         * Creates a [SpongeConfigManager] for managing a YAML configuration.
         *
         * @param C The type of the configuration class.
         * @param configClass The class of the configuration.
         * @param configFolder The folder where the configuration file is stored.
         * @param configFileName The name of the configuration file. Must match the YAML file name pattern.
         * @return A new instance of [SpongeConfigManager].
         */
        fun <C> yaml(
            configClass: Class<C>,
            configFolder: Path,
            configFileName: @YamlConfigFileNamePattern String
        ): SpongeConfigManager<C> = buildConfigManager(
            "https://yamlchecker.com/",
            YamlConfigurationLoader.builder().nodeStyle(NodeStyle.BLOCK),
            configClass,
            configFolder,
            configFileName
        )

        /**
         * Creates a [SpongeConfigManager] for managing a JSON configuration.
         *
         * @param C The type of the configuration class.
         * @param configClass The class of the configuration.
         * @param configFolder The folder where the configuration file is stored.
         * @param configFileName The name of the configuration file. Must match the JSON file name pattern.
         * @return A new instance of [SpongeConfigManager].
         */
        fun <C> json(
            configClass: Class<C>,
            configFolder: Path,
            configFileName: @JsonConfigFileNamePattern String
        ): SpongeConfigManager<C> = buildConfigManager(
            "https://jsonlint.com/",
            JacksonConfigurationLoader.builder(),
            configClass,
            configFolder,
            configFileName
        )


        private fun <C, T : AbstractConfigurationLoader.Builder<T, L>, L : AbstractConfigurationLoader<*>> buildConfigManager(
            verifyToolUrl: String,
            builder: AbstractConfigurationLoader.Builder<T, L>,
            configClass: Class<C>,
            configFolder: Path, configFileName: String
        ): SpongeConfigManager<C> {
            val loader = builder.path(configFolder.resolve(configFileName))
                .defaultOptions {
                    it.serializers(SpongeConfigSerializers.SERIALIZERS)
                        .shouldCopyDefaults(true)
                }
                .build()

            try {
                val node: ScopedConfigurationNode<*> = loader.load()
                val config = node.get(configClass) ?: throw LoadConfigException("Config is null after load")

                loader.save(node)
                node.set(configClass, config)

                return SpongeConfigManager(configClass, config, loader, node)
            } catch (e: SerializationException) {
                log.atSevere()
                    .withCause(e)
                    .log("Failed to load config due to serialization error")
                throw SerializationConfigException(e)
            } catch (e: ConfigurateException) {
                log.atSevere()
                    .withCause(e)
                    .log("Failed to load config")
                throw LoadConfigException(e)
            }
        }
    }
}

/**
 * Exception thrown when a configuration fails to load due to a critical error.
 */
class LoadConfigException : RuntimeException {

    /**
     * Constructs a new exception wrapping a [ConfigurateException].
     *
     * @param e The underlying exception.
     */
    constructor(e: ConfigurateException) : super(e)

    /**
     * Constructs a new exception with a custom error message.
     *
     * @param message The error message.
     */
    constructor(message: String) : super(message)

    companion object {
        @Serial
        private const val serialVersionUID = 9079792924817337725L
    }
}

/**
 * Exception thrown when a configuration fails to load due to serialization issues.
 */
class SerializationConfigException(e: SerializationException?) : RuntimeException(e) {
    companion object {
        @Serial
        private val serialVersionUID = -1667135777002012679L
    }
}