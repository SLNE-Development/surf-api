package dev.slne.surf.api.config.manager

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.config.ConfigFormat
import dev.slne.surf.api.config.migration.ConfigMigrations
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import org.spongepowered.configurate.ConfigurateException
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.loader.ConfigurationLoader
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializerCollection
import java.io.IOException
import java.io.Serial
import java.io.UncheckedIOException
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists
import kotlin.io.path.readText

/**
 * Reads, migrates and writes one configuration file with Configurate.
 *
 * Not thread-safe; [dev.slne.surf.api.config.ConfigHandle] serializes the access. [config] is
 * volatile, so readers on other threads see the value the last load, [reload] or assignment
 * installed.
 *
 * @param C the configuration class
 */
@InternalSurfApi
class SpongeConfigManager<C : Any> private constructor(
    private val configClass: Class<C>,
    private val path: Path,
    private val loader: ConfigurationLoader<out ConfigurationNode>,
    private val migrations: ConfigMigrations<C>,
    private var node: ConfigurationNode,
    /**
     * The current configuration.
     */
    @Volatile var config: C,
) {

    /**
     * Writes [config] to the file, keeping keys the class does not know.
     *
     * @throws UncheckedIOException if the file cannot be written
     */
    fun save() {
        try {
            node.set(configClass, config)
            loader.save(node)
        } catch (exception: ConfigurateException) {
            log.atSevere()
                .withCause(exception)
                .log("Failed to save config %s", configClass.name)
            throw UncheckedIOException(exception)
        }
    }

    /**
     * Reads the file into a candidate, migrates it in memory and installs it once [validate]
     * accepts it.
     *
     * The file is written only when a migration ran and the candidate was installed. A file that
     * holds no configuration, such as an emptied or deleted one, keeps [config] and logs a warning.
     *
     * @param validate checks the candidate; an exception it throws leaves [config] and the file
     * unchanged and propagates
     * @return the installed configuration
     * @throws UncheckedIOException if the file cannot be read, migrated or written, or does not
     * match [C]
     */
    fun reload(validate: (C) -> Unit): C {
        try {
            val candidateNode = if (isBlankFile()) null else loader.load()

            if (candidateNode == null || candidateNode.empty()) {
                log.atWarning()
                    .log(
                        "Config file of %s is empty or missing; keeping the current value",
                        configClass.name,
                    )

                return config
            }

            val isMigrated = migrations.migrate(candidateNode)
            val candidate = candidateNode.get(configClass) ?: return config

            validate(candidate)

            if (isMigrated) {
                candidateNode.set(configClass, candidate)
                loader.save(candidateNode)
            }

            node = candidateNode
            config = candidate

            return candidate
        } catch (exception: ConfigurateException) {
            log.atSevere()
                .withCause(exception)
                .log("Failed to reload config %s", configClass.name)
            throw UncheckedIOException(exception)
        } catch (exception: IOException) {
            log.atSevere()
                .withCause(exception)
                .log("Failed to read config %s", configClass.name)
            throw UncheckedIOException(exception)
        }
    }

    /**
     * Whether the file is missing or holds only whitespace, which not every format's loader
     * accepts.
     */
    private fun isBlankFile(): Boolean = path.notExists() || path.readText().isBlank()

    @InternalSurfApi
    companion object {
        private val log = logger()

        /**
         * Reads the file at [path], applying [migrations] and writing it back with the defaults
         * of [configClass].
         *
         * @param configClass the configuration class
         * @param path the file; it and its folder are created if missing
         * @param format the format of the file
         * @param migrations the migrations to apply
         * @param serializers the serializers of the file, for its whole lifetime
         * @return the manager holding the loaded configuration
         * @throws SerializationConfigException if the file does not match [configClass]
         * @throws LoadConfigException if the file cannot be read, migrated or written
         */
        fun <C : Any> open(
            configClass: Class<C>,
            path: Path,
            format: ConfigFormat,
            migrations: ConfigMigrations<C>,
            serializers: TypeSerializerCollection,
        ): SpongeConfigManager<C> {
            path.toAbsolutePath().parent?.createDirectories()
            val loader = format.loaderBuilder()
                .path(path)
                .defaultOptions { options ->
                    options.serializers(serializers)
                        .shouldCopyDefaults(true)
                }
                .build()

            try {
                val node = loader.load()
                migrations.migrate(node)
                val config = node.get(configClass)
                    ?: throw LoadConfigException("Config ${configClass.name} is empty after load")

                node.set(configClass, config)
                loader.save(node)

                return SpongeConfigManager(configClass, path, loader, migrations, node, config)
            } catch (exception: SerializationException) {
                log.atSevere()
                    .withCause(exception)
                    .log("Failed to deserialize config %s", configClass.name)
                throw SerializationConfigException(exception)
            } catch (exception: ConfigurateException) {
                log.atSevere()
                    .withCause(exception)
                    .log("Failed to load config %s", configClass.name)
                throw LoadConfigException(exception)
            }
        }
    }
}

/**
 * Thrown when a configuration file cannot be read, migrated or written while it is opened.
 */
class LoadConfigException : RuntimeException {

    /**
     * Creates the exception for a Configurate failure.
     *
     * @param e the failure
     */
    constructor(e: ConfigurateException) : super(e)

    /**
     * Creates the exception with a message.
     *
     * @param message the failure
     */
    constructor(message: String) : super(message)

    companion object {
        @Serial
        private const val serialVersionUID = 9079792924817337725L
    }
}

/**
 * Thrown when a configuration file does not match its class while it is opened.
 */
class SerializationConfigException(e: SerializationException?) : RuntimeException(e) {
    companion object {
        @Serial
        private val serialVersionUID = -1667135777002012679L
    }
}
