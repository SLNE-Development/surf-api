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

@PreferUsingSpongeConfigOverDazzlConf
class DazzlConfConfigManager<C> private constructor(private val helper: ConfigurationHelper<C>) {
    @Volatile
    var config: C? = null
        private set

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

    fun getOrCreateConfig(): C {
        val config = config ?: reloadConfig()
        return config
    }

    companion object {
        private val log = logger()

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
