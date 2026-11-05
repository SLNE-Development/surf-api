package dev.slne.surf.microservice.gradle

import org.gradle.api.provider.Property

/**
 * Controls the database integration that the plugin adds to the microservice.
 *
 * Both values are read when the dependencies are first resolved, so they may change after the plugin is applied.
 */
abstract class MicroserviceDatabaseExtension {
    /**
     * Whether `surf-microservice-database` and `surf-database-r2dbc` are added to `implementation`.
     *
     * Defaults to the Gradle property `surf.microservice.database.enabled`, otherwise `true`.
     */
    abstract val enabled: Property<Boolean>

    /**
     * Version of `dev.slne.surf:surf-database-r2dbc`.
     *
     * Defaults to the Gradle property `surf.database.version`, otherwise `2.+`.
     */
    abstract val version: Property<String>
}
