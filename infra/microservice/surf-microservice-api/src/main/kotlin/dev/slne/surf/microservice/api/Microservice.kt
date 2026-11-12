package dev.slne.surf.microservice.api

import dev.slne.surf.api.config.environment.env
import dev.slne.surf.microservice.api.command.MicroserviceCommands
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Base class of a microservice application.
 *
 * Every microservice module contains exactly one concrete subclass, either a class with a public no-argument
 * constructor or an `object`. The surf-microservice processor records it at compile time, so it needs no
 * registration and the module declares no `main` function.
 *
 * The runtime creates the instance before anything else starts. Constructors and property initializers must not touch
 * the database, NATS or other resources. [onBootstrap] runs once these are available.
 *
 * Start order:
 * 1. The built-in commands `exit` (alias `quit`), `health`, `help` and `spark` are registered.
 * 2. The instance is created and becomes [instance]. Its [name] is validated.
 * 3. surf-api and spark start.
 * 4. Every [MicroserviceIntegration][dev.slne.surf.microservice.api.integration.MicroserviceIntegration] starts.
 * 5. [onBootstrap] runs.
 * 6. The console and the command socket start accepting commands.
 *
 * Shutdown runs these steps in reverse for whatever started: the command endpoints stop, the integrations stop
 * accepting new work, [onDisable] runs, then the integrations, spark and surf-api stop. A failing step is
 * logged and the shutdown continues. A failed start runs the same shutdown and exits the process with code `1`.
 *
 * ```kotlin
 * object PunishMicroservice : Microservice() {
 *     override suspend fun onBootstrap(args: List<String>) {
 *         microserviceCommand("ping", description = "Answers with pong.") {
 *             sendLine("pong")
 *         }
 *     }
 *
 *     override suspend fun onDisable() {
 *         // Release what onBootstrap acquired.
 *     }
 * }
 * ```
 */
public abstract class Microservice {

    /**
     * Gets the name of this service, used for logs, the database pool and the migration history table.
     *
     * Must consist of lowercase letters and digits in groups separated by single hyphens, with at most
     * [MAX_NAME_LENGTH] characters. The runtime reads and validates it once at start, and an invalid name fails the
     * start with an [IllegalStateException]. Defaults to the environment variable [NAME_ENVIRONMENT_VARIABLE],
     * otherwise to the class name without the `Microservice` suffix in kebab case (`PunishMicroservice` becomes
     * `punish`).
     */
    public open val name: String
        get() = env.string(NAME_ENVIRONMENT_VARIABLE, defaultName())

    /**
     * Gets the directory that holds the configuration files of this service and its integrations.
     *
     * Defaults to the environment variable [CONFIG_DIR_ENVIRONMENT_VARIABLE], otherwise to `config`, relative to the
     * working directory.
     */
    public open val dataPath: Path
        get() = env.optional(CONFIG_DIR_ENVIRONMENT_VARIABLE, Path::of) ?: Path(DEFAULT_CONFIG_DIR)

    /**
     * Gets the registry of the console commands, the same object as [MicroserviceCommands].
     */
    public val commands: MicroserviceCommands
        get() = MicroserviceCommands

    /**
     * Starts the service logic after surf-api and all integrations have started.
     *
     * The console and the command socket accept commands only after this method returns. An exception aborts the
     * start: the runtime stops everything that already started without calling [onDisable] and exits with code `1`.
     * Release whatever this method acquired before it throws.
     *
     * @param args the command line arguments of the process
     */
    public open suspend fun onBootstrap(args: List<String>) {
    }

    /**
     * Stops the service logic during shutdown.
     *
     * Runs after the command endpoints stopped and every integration stopped accepting new work, such as incoming NATS
     * events and requests, and before the integrations stop. Publishing, calling services and database access still
     * work. Runs whenever [onBootstrap] returned, also if a later start step failed. An exception is logged and the
     * shutdown continues.
     */
    public open suspend fun onDisable() {
    }

    private fun defaultName(): String {
        val simpleName = javaClass.simpleName
            .removeSuffix(CLASS_NAME_SUFFIX)
            .ifEmpty { CLASS_NAME_SUFFIX }

        return simpleName.replace(WORD_BOUNDARY, "-").lowercase()
    }

    public companion object {
        /**
         * Names the environment variable that overrides the default [name].
         */
        public const val NAME_ENVIRONMENT_VARIABLE: String = "SURF_MICROSERVICE_NAME"

        /**
         * Names the environment variable that overrides the default [dataPath].
         */
        public const val CONFIG_DIR_ENVIRONMENT_VARIABLE: String = "SURF_MICROSERVICE_CONFIG_DIR"

        /**
         * Maximum length of [name] in characters.
         */
        public const val MAX_NAME_LENGTH: Int = 40

        private const val DEFAULT_CONFIG_DIR = "config"
        private const val CLASS_NAME_SUFFIX = "Microservice"
        private val WORD_BOUNDARY = Regex("(?<=[a-z0-9])(?=[A-Z])")

        @Volatile
        private var current: Microservice? = null

        /**
         * Gets the running microservice.
         *
         * Available from the moment the runtime created the instance, before any integration starts. Use
         * [getMicroservice] to get it as its concrete type.
         *
         * @throws IllegalStateException if the runtime has not created the microservice yet
         */
        public val instance: Microservice
            get() = checkNotNull(current) { "No microservice is running" }

        /**
         * Sets the running microservice. Called once by the runtime after creating it.
         */
        @InternalMicroserviceApi
        public fun install(microservice: Microservice) {
            current = microservice
        }
    }
}
