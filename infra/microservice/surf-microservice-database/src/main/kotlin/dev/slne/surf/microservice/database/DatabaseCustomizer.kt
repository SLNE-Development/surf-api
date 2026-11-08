package dev.slne.surf.microservice.database

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig
import dev.slne.surf.database.migration.MigrationSpec

/**
 * Customizes the database of a microservice; implemented by the [Microservice][dev.slne.surf.microservice.api.Microservice]
 * subclass itself.
 *
 * Without this interface the database integration uses its defaults: the configuration from `database.yml` in the
 * data path and migrations found by convention (SQL files in `db/migration` and the `SurfMigration` classes of the
 * module). The hooks run at most once, during the start of the database integration and before `onBootstrap`.
 *
 * A module that needs different SQL per database vendor puts it in `db/migration/mariadb` and
 * `db/migration/postgresql`; then only the folder of the configured vendor runs and files directly in `db/migration`
 * are ignored. Start fails if vendor folders exist but none for the configured vendor.
 */
interface DatabaseCustomizer {

    /**
     * Adjusts the Exposed configuration after the defaults have been applied.
     */
    fun R2dbcDatabaseConfig.Builder.configureDatabase() {
    }

    /**
     * Adds to or overrides the migrations found by convention.
     *
     * The class loader is already set to the one of the microservice class. Not called if [runMigrations] is `false`.
     */
    fun MigrationSpec.configureMigrations() {
    }

    /**
     * Whether migrations run at start.
     *
     * When `true`, they run even if no migration was found by convention, so [configureMigrations] must then declare
     * at least one. `false` skips migrations entirely, for services that still create their schema with `SchemaUtils`.
     */
    val runMigrations: Boolean
        get() = true
}
