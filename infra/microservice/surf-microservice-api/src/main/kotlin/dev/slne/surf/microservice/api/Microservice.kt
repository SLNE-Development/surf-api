package dev.slne.surf.microservice.api

import dev.slne.surf.api.core.environment.env
import dev.slne.surf.microservice.api.command.MicroserviceCommands
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Base class of a microservice application.
 *
 * Every microservice module contains exactly one concrete subclass, either a class with a public no-argument
 * constructor or an `object`. The surf-microservice processor finds it at compile time, so it needs no registration.
 *
 * The runtime creates the instance before anything else is started. Constructors and property initializers must
 * therefore not touch the database, NATS or other resources; [onBootstrap] runs once these are available.
 */
abstract class Microservice {

    /**
     * Name of this service, used for logs, the database pool and the migration history table.
     *
     * Must consist of lowercase letters, digits and single hyphens, with at most [MAX_NAME_LENGTH] characters.
     * Defaults to the environment variable [NAME_ENVIRONMENT_VARIABLE], otherwise to the class name without the
     * `Microservice` suffix in kebab case (`PunishMicroservice` becomes `punish`).
     */
    open val name: String
        get() = env.string(NAME_ENVIRONMENT_VARIABLE, defaultName())

    /**
     * Directory holding the configuration files of this service and its integrations.
     *
     * Defaults to the environment variable [CONFIG_DIR_ENVIRONMENT_VARIABLE], otherwise to `config`.
     */
    open val dataPath: Path
        get() = env.optional(CONFIG_DIR_ENVIRONMENT_VARIABLE, Path::of) ?: Path(DEFAULT_CONFIG_DIR)

    /**
     * Console commands of this service.
     */
    val commands: MicroserviceCommands
        get() = MicroserviceCommands

    /**
     * Called after surf-api and all integrations have started.
     *
     * An exception aborts the start; the runtime then stops everything that was already started, without calling
     * [onDisable]. Release whatever this method acquired before it throws.
     */
    open suspend fun onBootstrap(args: List<String>) {
    }

    /**
     * Called during shutdown after NATS stopped accepting new requests and before the integrations stop.
     *
     * Runs whenever [onBootstrap] returned, also if a later start step failed. Publishing and database access are
     * still possible.
     */
    open suspend fun onDisable() {
    }

    private fun defaultName(): String {
        val simpleName = javaClass.simpleName
            .removeSuffix(CLASS_NAME_SUFFIX)
            .ifEmpty { CLASS_NAME_SUFFIX }

        return simpleName.replace(WORD_BOUNDARY, "-").lowercase()
    }

    companion object {
        const val NAME_ENVIRONMENT_VARIABLE = "SURF_MICROSERVICE_NAME"
        const val CONFIG_DIR_ENVIRONMENT_VARIABLE = "SURF_MICROSERVICE_CONFIG_DIR"
        const val MAX_NAME_LENGTH = 40

        private const val DEFAULT_CONFIG_DIR = "config"
        private const val CLASS_NAME_SUFFIX = "Microservice"
        private val WORD_BOUNDARY = Regex("(?<=[a-z0-9])(?=[A-Z])")

        @Volatile
        private var current: Microservice? = null

        /**
         * The running microservice.
         *
         * @throws IllegalStateException if the runtime has not created the microservice yet
         */
        val instance: Microservice
            get() = checkNotNull(current) { "No microservice is running" }

        /**
         * Sets the running microservice; called once by the runtime after creating it.
         */
        @InternalMicroserviceApi
        fun install(microservice: Microservice) {
            current = microservice
        }
    }
}
