package dev.slne.surf.microservice.gradle

import org.gradle.api.provider.Property

/**
 * Controls the database integration that the plugin adds to the microservice.
 *
 * With the integration enabled, the plugin adds `surf-api-microservice-database` and
 * `dev.slne.surf.api:surf-api-database-r2dbc` to `implementation`. Both settings are read when `implementation` is
 * first resolved, so they may be set after the plugin is applied.
 *
 * ```kotlin
 * surfMicroservice {
 *     database {
 *         // Pin a database release instead of the default, the major version of the plugin
 *         version = "4.0.0"
 *     }
 * }
 * ```
 *
 * A microservice without a database turns the integration off:
 *
 * ```kotlin
 * surfMicroservice {
 *     database {
 *         enabled = false
 *     }
 * }
 * ```
 *
 * The same settings can come from `gradle.properties` or `-P` on the command line:
 *
 * ```properties
 * surf.microservice.database.enabled=false
 * surf.database.version=4.0.0
 * ```
 */
abstract class MicroserviceDatabaseExtension {
    /**
     * Whether the database integration is added to the project.
     *
     * The default is the Gradle property `surf.microservice.database.enabled` (`true` or `false`, any other value
     * fails the build), otherwise `true`. With the integration off, the reliability integration is off by default too.
     */
    abstract val enabled: Property<Boolean>

    /**
     * Version of `dev.slne.surf.api:surf-api-database-r2dbc`, a Gradle version notation such as `4.0.0` or `4.+`.
     *
     * The default is the Gradle property `surf.database.version`, otherwise the major version of the plugin,
     * for example `4.+`.
     */
    abstract val version: Property<String>
}
