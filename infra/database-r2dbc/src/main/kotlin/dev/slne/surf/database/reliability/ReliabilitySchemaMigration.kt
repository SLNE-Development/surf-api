package dev.slne.surf.database.reliability

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.config.DatabaseType
import dev.slne.surf.database.migration.MigrationResult
import dev.slne.surf.database.migration.migrate
import dev.slne.surf.database.reliability.internal.MariaDbRequirements
import dev.slne.surf.database.reliability.internal.ReliabilitySchema

/**
 * Creates or updates `surf_atomic_operations`, the table of
 * [AtomicOperations][dev.slne.surf.database.reliability.operation.AtomicOperations].
 *
 * The migrations belong to the library, live under `db/surf-reliability/{vendor}` and keep their own history in
 * `flyway_schema_history_surf_reliability`, independent of the application's migrations. Call this before the table
 * is used, also when the application does not migrate its own schema; repeated calls are no-ops. The table is shared
 * by all owners on the database.
 *
 * On MariaDB, the server must be MariaDB 10.6 or newer and the table must use InnoDB; both are checked here.
 *
 * Same constraints as [migrate]: not inside an open transaction.
 *
 * @throws IllegalStateException if the MariaDB server or a table does not meet the requirements, or a migration fails
 */
suspend fun DatabaseApi.migrateReliabilitySchema(): MigrationResult {
    val isMariaDb = databaseType == DatabaseType.MARIADB
    if (isMariaDb) {
        MariaDbRequirements.requireSupportedVersion(ReliabilitySchema.serverVersion(this))
    }

    val result = migrate(ReliabilitySchema.MIGRATION_NAME) {
        sql(ReliabilitySchema.SQL_LOCATION)
    }

    if (isMariaDb) {
        MariaDbRequirements.requireInnoDb(ReliabilitySchema.mariaDbTableEngines(this))
    }

    return result
}
