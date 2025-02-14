package dev.slne.surf.surfapi.core.api.config.manager

import dev.slne.surf.surfapi.core.api.config.YamlConfigFileNamePattern
import dev.slne.surf.surfapi.core.api.config.serializer.DefaultDazzlConfSerializers
import dev.slne.surf.surfapi.core.api.util.logger
import space.arim.dazzleconf.ConfigurationOptions
import space.arim.dazzleconf.error.ConfigFormatSyntaxException
import space.arim.dazzleconf.error.InvalidConfigException
import space.arim.dazzleconf.ext.snakeyaml.CommentMode
import space.arim.dazzleconf.ext.snakeyaml.SnakeYamlConfigurationFactory
import space.arim.dazzleconf.ext.snakeyaml.SnakeYamlOptions
import space.arim.dazzleconf.helper.ConfigurationHelper
import space.arim.dazzleconf.sorter.AnnotationBasedSorter
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.TimeUnit

@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "Prefer using Sponge's Configurate library over DazzlConf"
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class PreferUsingSpongeConfigOverDazzlConf

/**
 * Manages configurations using the DazzlConf library, including loading, saving, and reloading configurations.
 * Provides resilience against syntax or invalid data errors and defaults to a valid configuration when such errors occur.
 *
 * @param C The type of the configuration class.
 * @property config The current configuration instance, or `null` if not yet loaded.
 */
@PreferUsingSpongeConfigOverDazzlConf
class DazzlConfConfigManager<C> private constructor(private val helper: ConfigurationHelper<C>) {
    @Volatile
    var config: C? = null
        private set

    /**
     * Reloads the configuration from the file.
     * If a syntax or validation error occurs, a default configuration is used.
     *
     * @return The reloaded configuration instance.
     * @throws RuntimeException if an I/O error or other critical issue occurs.
     */
    fun reloadConfig(): C {
        try {
            config = helper.reloadConfigData()
        } catch (e: IOException) {
            log.atSevere()
                .withCause(e)
                .atMostEvery(10, TimeUnit.SECONDS)
                .log("Failed to reload config")
            throw RuntimeException(e)
        } catch (e: ConfigFormatSyntaxException) {
            config = helper.getFactory().loadDefaults()
            log.atSevere()
                .withCause(e)
                .log(
                    """
                    Failed to reload config due to syntax error.
                    Using default config instead.
                    Check the YAML syntax with a tool like https://yamlchecker.com/
                    """.trimIndent()
                )
        } catch (e: InvalidConfigException) {
            config = helper.getFactory().loadDefaults()
            log.atSevere()
                .withCause(e)
                .log(
                    """
                    Failed to reload config due to invalid config.
                    Using default config instead.
                    Check the config values and try again.
                    """.trimIndent()
                )
        }

        return config ?: error("Config is null after reload")
    }

    /**
     * Retrieves the current configuration, reloading it if not already loaded.
     *
     * @return The configuration instance.
     */
    fun getOrCreateConfig(): C {
        val config = config ?: reloadConfig()
        return config
    }

    companion object {
        private val log = logger()

        /**
         * Creates a new instance of [DazzlConfConfigManager] for managing a YAML configuration.
         *
         * @param C The type of the configuration class.
         * @param configClass The class of the configuration.
         * @param configFolder The folder where the configuration file is stored.
         * @param configFileName The name of the configuration file. Must match the YAML file name pattern.
         * @return A new instance of [DazzlConfConfigManager].
         */
        @JvmStatic
        fun <C> create(
            configClass: Class<C>,
            configFolder: Path,
            configFileName: @YamlConfigFileNamePattern String
        ): DazzlConfConfigManager<C> {
            val options = SnakeYamlOptions.Builder()
                .commentMode(CommentMode.fullComments())
                .build()

            val factory = SnakeYamlConfigurationFactory.create(
                configClass,
                ConfigurationOptions.Builder()
                    .addSerialisers(DefaultDazzlConfSerializers.DEFAULTS)
                    .setCreateSingleElementCollections(true)
                    .sorter(AnnotationBasedSorter())
                    .build(),
                options
            )

            return DazzlConfConfigManager(
                ConfigurationHelper(
                    configFolder,
                    configFileName,
                    factory
                )
            )
        }
    }
}
