package dev.slne.surf.api.spring

/**
 * Names of the Spring properties that several surf modules publish or read.
 *
 * The values are compile-time constants, so a module that uses one needs this API only at compile time.
 */
object SurfProperties {

    /**
     * The directory of the configuration files of surf modules, such as the database's `database.yml` and NATS's
     * `nats/config.yml`.
     *
     * A microservice publishes it from `SURF_MICROSERVICE_CONFIG_DIR` ahead of every other property source, so the
     * environment variable `SURF_CONFIG_DIRECTORY` has no effect there. Elsewhere the property comes from the
     * application's own property sources. Modules that read it fall back to `config` or leave their defaults.
     */
    const val CONFIG_DIRECTORY = "surf.config-directory"

    /**
     * The directory modules use when [CONFIG_DIRECTORY] is not set, relative to the working directory.
     */
    const val DEFAULT_CONFIG_DIRECTORY = "config"

    /**
     * Spring's own property for the name of the application. A microservice publishes its name under it, and the
     * database names its connection pool after it.
     */
    const val APPLICATION_NAME = "spring.application.name"
}
