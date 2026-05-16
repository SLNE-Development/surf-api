package dev.slne.surf.api.core.config.manager

import dev.slne.surf.api.core.config.JsonConfigFileNamePattern
import dev.slne.surf.api.core.config.YamlConfigFileNamePattern
import dev.slne.surf.api.core.config.manager.SpongeConfigManager.Companion.json
import dev.slne.surf.api.core.config.manager.SpongeConfigManager.Companion.yaml
import dev.slne.surf.api.core.config.migration.ConfigMigration
import dev.slne.surf.api.core.config.migration.ConfigMigrationBuilder
import dev.slne.surf.api.core.config.serializer.surfSpongeConfigSerializers
import dev.slne.surf.api.core.util.logger
import io.leangen.geantyref.GenericTypeReflector
import org.spongepowered.configurate.CommentedConfigurationNodeIntermediary
import org.spongepowered.configurate.ConfigurateException
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.ScopedConfigurationNode
import org.spongepowered.configurate.jackson.JacksonConfigurationLoader
import org.spongepowered.configurate.kotlin.dataClassFieldDiscoverer
import org.spongepowered.configurate.loader.AbstractConfigurationLoader
import org.spongepowered.configurate.loader.ConfigurationLoader
import org.spongepowered.configurate.objectmapping.ObjectMapper
import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.objectmapping.meta.NodeResolver
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializerCollection
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.Serial
import java.io.UncheckedIOException
import java.nio.file.Path
import java.text.MessageFormat
import java.util.*
import java.util.regex.Pattern

/**
 * Manages configurations using Sponge's Configurate library, including loading, saving, and reloading configurations.
 * Supports multiple formats, including YAML and JSON.
 *
 * Optionally supports **versioned migrations** via [ConfigMigrationBuilder]. When migrations are
 * registered (either via the builder overloads of [yaml]/[json] or after construction via
 * [addMigration]), they are applied automatically on first load and on every [reloadFromFile].
 *
 * @param C The type of the configuration class.
 * @property config The current configuration instance.
 */
class SpongeConfigManager<C> private constructor(
    private val configClass: Class<C>,
    @JvmField @field:Volatile var config: C,
    private val loader: ConfigurationLoader<out ConfigurationNode>,
    private val node: ConfigurationNode,
    private val migrationBuilder: ConfigMigrationBuilder
) {

    /**
     * Saves the current configuration to the file.
     *
     * @throws UncheckedIOException if an I/O error or serialization error occurs.
     */
    @Throws(UncheckedIOException::class)
    fun save() {
        try {
            node.set(configClass, config)
            loader.save(node)
        } catch (e: ConfigurateException) {
            log.atSevere()
                .withCause(e)
                .log("Failed to save config")
            throw UncheckedIOException(e)
        }
    }

    /**
     * Reloads the configuration from the file. If loading fails, the current configuration remains unchanged.
     *
     * Migrations are applied automatically if any are registered.
     *
     * @return The reloaded configuration instance.
     * @throws UncheckedIOException if an I/O error occurs during reload.
     */
    fun reloadFromFile(): C {
        try {
            val reloadedNode: ConfigurationNode = loader.load()

            // Apply migrations before deserializing
            applyMigrations(reloadedNode)

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
            throw UncheckedIOException(e)
        }
    }

    /**
     * Allows editing the configuration by applying changes within the provided block.
     * Optionally saves the changes to the configuration file after applying the modifications.
     *
     * @param save Indicates whether the configuration should be saved after applying changes. Defaults to `true`.
     * @param block A lambda with receiver scope of the configuration type to apply modifications.
     */
    inline fun edit(save: Boolean = true, block: C.() -> Unit) {
        config.block()
        if (save) {
            save()
        }
    }

    /**
     * Registers a migration for the given target version.
     *
     * This can be called after construction to add migrations dynamically.
     * Note: Migrations added after initial load will only take effect on the next [reloadFromFile].
     *
     * @param version the target version this migration upgrades to (must be >= 0)
     * @param migration the migration to apply
     * @return this manager for chaining
     */
    fun addMigration(version: Int, migration: ConfigMigration): SpongeConfigManager<C> {
        migrationBuilder.migration(version, migration)
        return this
    }

    /**
     * Registers an inline migration for the given target version.
     *
     * @param version the target version this migration upgrades to (must be >= 0)
     * @param migration the migration lambda
     * @return this manager for chaining
     */
    inline fun addMigration(
        version: Int,
        crossinline migration: (ConfigurationNode) -> Unit
    ): SpongeConfigManager<C> {
        return addMigration(version, ConfigMigration { node -> migration(node) })
    }

    /**
     * Returns the [ConfigMigrationBuilder] used by this manager.
     *
     * Can be used to inspect registered migrations or customize the version key.
     */
    fun migrations(): ConfigMigrationBuilder = migrationBuilder

    /**
     * Applies pending migrations to the given node and saves if any were applied.
     */
    private fun applyMigrations(node: ConfigurationNode) {
        if (!migrationBuilder.hasMigrations()) return

        try {
            val result = migrationBuilder.migrate(node)
            if (result.migrated) {
                // Save the migrated node immediately so the version is persisted
                loader.save(node)
            }
        } catch (e: ConfigurateException) {
            log.atSevere()
                .withCause(e)
                .log("Failed to apply config migrations")
            throw e
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
         * @param migrations Optional migration builder with pre-registered migrations.
         * @return A new instance of [SpongeConfigManager].
         */
        @JvmOverloads
        fun <C> yaml(
            configClass: Class<C>,
            configFolder: Path,
            configFileName: @YamlConfigFileNamePattern String,
            migrations: ConfigMigrationBuilder = ConfigMigrationBuilder()
        ): SpongeConfigManager<C> = buildConfigManager(
            "https://yamlchecker.com/",
            YamlConfigurationLoader.builder().nodeStyle(NodeStyle.BLOCK).commentsEnabled(true),
            configClass,
            configFolder,
            configFileName,
            migrations
        )

        /**
         * Creates a [SpongeConfigManager] for managing a JSON configuration.
         *
         * @param C The type of the configuration class.
         * @param configClass The class of the configuration.
         * @param configFolder The folder where the configuration file is stored.
         * @param configFileName The name of the configuration file. Must match the JSON file name pattern.
         * @param migrations Optional migration builder with pre-registered migrations.
         * @return A new instance of [SpongeConfigManager].
         */
        @JvmOverloads
        fun <C> json(
            configClass: Class<C>,
            configFolder: Path,
            configFileName: @JsonConfigFileNamePattern String,
            migrations: ConfigMigrationBuilder = ConfigMigrationBuilder()
        ): SpongeConfigManager<C> = buildConfigManager(
            "https://jsonlint.com/",
            JacksonConfigurationLoader.builder(),
            configClass,
            configFolder,
            configFileName,
            migrations
        )


        private fun <C, T : AbstractConfigurationLoader.Builder<T, L>, L : AbstractConfigurationLoader<*>> buildConfigManager(
            verifyToolUrl: String,
            builder: AbstractConfigurationLoader.Builder<T, L>,
            configClass: Class<C>,
            configFolder: Path,
            configFileName: String,
            migrations: ConfigMigrationBuilder
        ): SpongeConfigManager<C> {
            val loader = builder.path(configFolder.resolve(configFileName))
                .defaultOptions {
                    it.serializers { serializers ->
                        surfSpongeConfigSerializers.buildSerializersModule().accept(serializers)

                        try {
                            OldSpongeReflections.OLD_CONFIG_SERIALIZABLE_ANNOTATION
                            serializers.registerBackwardsCompatibleSerializers()
                        } catch (_: Exception) {
                            // no none relocated annotations, no need to register
                        }
                    }.shouldCopyDefaults(true)
                }
                .build()

            try {
                val node: ScopedConfigurationNode<*> = loader.load()

                // Apply migrations before deserializing into the config class.
                // This handles both:
                // - Existing configs without a version field (version = -1, all migrations run)
                // - Configs with an older version (only newer migrations run)
                if (migrations.hasMigrations()) {
                    val result = migrations.migrate(node)
                    if (result.migrated) {
                        loader.save(node) // persist migration + version field
                    }
                }

                val config =
                    node.get(configClass) ?: throw LoadConfigException("Config is null after load")

                // Re-save to ensure defaults are written
                node.set(configClass, config)
                loader.save(node)

                return SpongeConfigManager(configClass, config, loader, node, migrations)
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

        private fun TypeSerializerCollection.Builder.registerBackwardsCompatibleSerializers() {
            register(
                { type ->
                    GenericTypeReflector.annotate(type)
                        .isAnnotationPresent(OldSpongeReflections.OLD_CONFIG_SERIALIZABLE_ANNOTATION)
                },
                ObjectMapper.factoryBuilder()
                    .addDiscoverer(dataClassFieldDiscoverer())
                    .addProcessor(OldSpongeReflections.OLD_COMMENT_ANNOTATION) { data, _ ->
                        { _, destination ->
                            if (destination is CommentedConfigurationNodeIntermediary<*>) {
                                if (OldSpongeReflections.isCommentOverride(data)) {
                                    destination.comment(OldSpongeReflections.getCommentValue(data))
                                } else {
                                    destination.commentIfAbsent(OldSpongeReflections.getCommentValue(data))
                                }
                            }
                        }
                    }
                    .addConstraint(
                        OldSpongeReflections.OLD_MATCHES_ANNOTATION,
                        String::class.java
                    ) { data, _ ->
                        val value = OldSpongeReflections.getMatchesValue(data)
                        val flags = OldSpongeReflections.getMatchesFlags(data)
                        val failureMessage = OldSpongeReflections.getMatchesFailureMessage(data)

                        val test = Pattern.compile(value, flags)
                        val format = MessageFormat(failureMessage, Locale.getDefault())

                        Constraint { toValidate ->
                            if (toValidate != null) {
                                val match = test.matcher(toValidate)
                                if (!match.matches()) {
                                    throw SerializationException(format.format(arrayOf(toValidate, value)))
                                }
                            }
                        }
                    }
                    .addConstraint(OldSpongeReflections.OLD_REQUIRED_ANNOTATION, Constraint.required())
                    .addNodeResolver(fun(name, element): NodeResolver? {
                        if (element.isAnnotationPresent(OldSpongeReflections.OLD_SETTING_ANNOTATION)) {
                            val annotation =
                                element.getAnnotation(OldSpongeReflections.OLD_SETTING_ANNOTATION)
                            val key = OldSpongeReflections.getSettingValue(annotation)
                            if (key.isNotEmpty()) {
                                return { node -> node.node(key) }
                            }
                        }

                        return null
                    })
                    .addNodeResolver(fun(name, element): NodeResolver? {
                        if (element.isAnnotationPresent(OldSpongeReflections.OLD_SETTING_ANNOTATION)) {
                            val annotation =
                                element.getAnnotation(OldSpongeReflections.OLD_SETTING_ANNOTATION)
                            val nodeFromParent = OldSpongeReflections.isSettingNodeFromParent(annotation)
                            if (nodeFromParent) {
                                return { node -> node }
                            }
                        }

                        return null
                    })
                    .build()
                    .asTypeSerializer()
            )
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