package dev.slne.surf.surfapi.core.api.config

import dev.slne.surf.surfapi.core.api.config.serializer.SpongeConfigSerializers
import dev.slne.surf.surfapi.core.api.util.logger
import org.intellij.lang.annotations.Language
import org.intellij.lang.annotations.Pattern
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

class SpongeConfigManager<C> @Contract(pure = true) private constructor(
    private val configClass: Class<C>,
    @JvmField @field:Volatile var config: C,
    private val loader: ConfigurationLoader<out ConfigurationNode>,
    private val node: ConfigurationNode
) {
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

    @Pattern(YAML_CONFIG_FILE_NAME_PATTERN)
    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER)
    @MustBeDocumented
    annotation class YamlConfigFileNamePattern

    @Pattern(JSON_CONFIG_FILE_NAME_PATTERN)
    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER)
    @MustBeDocumented
    annotation class JsonConfigFileNamePattern

    companion object {
        private val log = logger()

        @Language("RegExp")
        private const val YAML_CONFIG_FILE_NAME_PATTERN = "^[a-zA-Z0-9_-]+\\.(yml|yaml)$"

        @Language("RegExp")
        private const val JSON_CONFIG_FILE_NAME_PATTERN = "^[a-zA-Z0-9_-]+\\.(json)$"

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

class LoadConfigException : RuntimeException {
    constructor(e: ConfigurateException) : super(e)
    constructor(message: String) : super(message)

    companion object {
        @Serial
        private const val serialVersionUID = 9079792924817337725L
    }
}

class SerializationConfigException(e: SerializationException?) : RuntimeException(e) {
    companion object {
        @Serial
        private val serialVersionUID = -1667135777002012679L
    }
}