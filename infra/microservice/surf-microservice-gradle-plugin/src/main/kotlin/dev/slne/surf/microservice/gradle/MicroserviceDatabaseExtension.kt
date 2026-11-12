package dev.slne.surf.microservice.gradle

import org.gradle.api.provider.Property

/**
 * Controls the database integration that the plugin adds to the microservice.
 *
 * With the integration enabled, the plugin adds `surf-microservice-database` and `dev.slne.surf:surf-database-r2dbc`
 * to `implementation`. Both settings are read when `implementation` is first resolved, so they may be set after the
 * plugin is applied.
 *
 * ```kotlin
 * surfMicroservice {
 *     database {
 *         // Pin a surf-database release instead of the default 2.+
 *         version = "2.4.2"
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
 * surf.database.version=2.4.2
 * ```
 *
 * The build fails if surf-api's `withSurfDatabaseR2dbc(...)` is also used, because it adds `surf-database-r2dbc` to
 * `api` with an unpinned version. Remove that call and set [version] here instead.
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
     * Version of `dev.slne.surf:surf-database-r2dbc`, a Gradle version notation such as `2.4.2` or `2.+`.
     *
     * The default is the Gradle property `surf.database.version`, otherwise `2.+`.
     */
    abstract val version: Property<String>
}
