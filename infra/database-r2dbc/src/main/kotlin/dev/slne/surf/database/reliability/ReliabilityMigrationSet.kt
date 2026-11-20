package dev.slne.surf.database.reliability

import dev.slne.surf.database.config.DatabaseType
import dev.slne.surf.database.config.databaseType
import dev.slne.surf.database.migration.MigrationSet
import dev.slne.surf.database.migration.MigrationSpec
import dev.slne.surf.database.reliability.internal.MariaDbRequirements
import dev.slne.surf.database.reliability.internal.ReliabilitySchema
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase

/**
 * Creates or updates `surf_atomic_operations`, the table of
 * [AtomicOperations][dev.slne.surf.database.reliability.operation.AtomicOperations].
 *
 * The migrations belong to the library, live under `db/surf-reliability/{vendor}` and keep their own history in
 * `flyway_schema_history_surf_reliability`, independent of the application's migrations. Register the set as a bean
 * wherever `AtomicOperations` is used, also when the application does not migrate its own schema. The table is
 * shared by all owners on the database.
 *
 * On MariaDB, the server must be MariaDB 10.6 or newer and the table must use InnoDB; both are checked around the
 * migrations.
 */
class ReliabilityMigrationSet : MigrationSet {

    override val name: String = ReliabilitySchema.MIGRATION_NAME

    override fun MigrationSpec.describe() {
        sql(ReliabilitySchema.SQL_LOCATION)
    }

    override suspend fun beforeMigrate(database: R2dbcDatabase) {
        if (database.databaseType == DatabaseType.MARIADB) {
            MariaDbRequirements.requireSupportedVersion(ReliabilitySchema.serverVersion(database))
        }
    }

    override suspend fun afterMigrate(database: R2dbcDatabase) {
        if (database.databaseType == DatabaseType.MARIADB) {
            MariaDbRequirements.requireInnoDb(ReliabilitySchema.mariaDbTableEngines(database))
        }
    }
}
