package dev.slne.surf.microservice.database

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig
import dev.slne.surf.database.migration.MigrationSpec

/**
 * Customizes the database of a microservice.
 *
 * Implement it on the [Microservice][dev.slne.surf.microservice.api.Microservice] subclass itself. The database
 * integration ignores the interface on any other class.
 *
 * Without this interface the database integration uses its defaults:
 * - The connection settings come from `database.yml` in the
 *   [data path][dev.slne.surf.microservice.api.Microservice.dataPath].
 * - The connection pool is named after the service.
 * - The migrations are those found by convention: the SQL files in the classpath folder `db/migration` and the
 *   `SurfMigration` classes of the module, which the surf-microservice processor lists at compile time.
 * - The migration history lives in the table `flyway_schema_history_<name>`, with every `-` of the service name
 *   replaced by `_`.
 * - If surf-nats is present, the default number of concurrent NATS handlers per service follows the pool size: twice
 *   the pool size in total, half of it (at least one) for long-running calls. The NATS configuration overrides both.
 *
 * A module that needs different SQL per database vendor puts it in `db/migration/mariadb` and
 * `db/migration/postgresql`. Then only the folder of the configured vendor runs, and files directly in `db/migration`
 * are ignored. The start fails if vendor folders exist but none for the configured vendor.
 *
 * The members are read at most once, while the database integration starts and before `onBootstrap`.
 *
 * ```kotlin
 * object PunishMicroservice : Microservice(), DatabaseCustomizer {
 *     override fun R2dbcDatabaseConfig.Builder.configureDatabase() {
 *         defaultMaxAttempts = 5
 *     }
 *
 *     override fun MigrationSpec.configureMigrations() {
 *         sql("db/legacy")
 *         baselineVersion("1")
 *     }
 * }
 * ```
 */
public interface DatabaseCustomizer {

    /**
     * Adjusts the Exposed configuration of the database.
     *
     * Runs after surf-database applied its defaults, such as READ COMMITTED as the default isolation level, so a
     * value set here replaces the default.
     */
    public fun R2dbcDatabaseConfig.Builder.configureDatabase() {
    }

    /**
     * Adds migrations to those found by convention, or changes the baseline version or the class loader.
     *
     * The spec already contains the conventional migrations, and its class loader is the one of the microservice
     * class. Not called if [runMigrations] is `false`.
     */
    public fun MigrationSpec.configureMigrations() {
    }

    /**
     * Returns whether migrations run at start.
     *
     * When `true` (the default), they run even if no migration was found by convention, so [configureMigrations] must
     * then declare at least one. `false` skips the migrations of the service, both the conventional ones and those
     * from [configureMigrations], for services that still create their schema with `SchemaUtils`. Library schemas,
     * such as the one of surf-microservice-reliability, are still migrated.
     */
    public val runMigrations: Boolean
        get() = true
}
