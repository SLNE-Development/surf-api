package dev.slne.surf.microservice.api

import java.nio.file.Path

/**
 * The identity of the running microservice, available as a bean.
 *
 * @property name the service name, used for logs, the database pool, the migration history table, the NATS client and
 * the owner of stored replies; lowercase letters and digits in groups separated by single hyphens, at most
 * [MAX_NAME_LENGTH] characters
 * @property dataPath the directory that holds the configuration files of the service and its integrations
 */
public class MicroserviceInfo(
    public val name: String,
    public val dataPath: Path,
) {
    override fun toString(): String = "MicroserviceInfo(name=$name, dataPath=$dataPath)"

    public companion object {
        /**
         * Names the environment variable that sets [name].
         */
        public const val NAME_ENVIRONMENT_VARIABLE: String = "SURF_MICROSERVICE_NAME"

        /**
         * Names the environment variable that sets [dataPath]; the default is `config`, relative to the working
         * directory.
         */
        public const val CONFIG_DIR_ENVIRONMENT_VARIABLE: String = "SURF_MICROSERVICE_CONFIG_DIR"

        /**
         * Maximum length of [name] in characters.
         */
        public const val MAX_NAME_LENGTH: Int = 40
    }
}
