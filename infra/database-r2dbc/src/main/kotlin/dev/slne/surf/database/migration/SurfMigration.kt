package dev.slne.surf.database.migration

import dev.slne.surf.database.DatabaseApi
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import java.sql.Connection

/**
 * A code migration shipped by a plugin; extend [JdbcMigration] or [ExposedMigration].
 *
 * [version] and [description] order and label the migration in the plugin's history table, like the
 * `V<version>__<description>.sql` name of a SQL migration. Migrations must be idempotent, for example
 * `CREATE TABLE IF NOT EXISTS`, because the database may already contain objects created before the plugin's history
 * started.
 *
 * When a migration throws, `migrate()` fails. On PostgreSQL no history row is left. On MariaDB the migration is
 * recorded as failed, and every later `migrate()` of the plugin fails until that row is deleted manually; there is no
 * repair API.
 *
 * @property version Dot-separated numbers, for example `"2"` or `"2.1"`.
 */
sealed class SurfMigration(val version: String, val description: String) {
    /**
     * Checksum stored with the applied migration; a later run with a different checksum fails validation. `null`
     * disables the check.
     */
    open val checksum: Int? = null
}

/**
 * A migration written against plain JDBC.
 *
 * [migrate] runs inside the transaction of the migration run; the connection must not be committed or closed. If it
 * throws, PostgreSQL rolls back everything including DDL, while MariaDB keeps any DDL that already ran.
 */
abstract class JdbcMigration(version: String, description: String) : SurfMigration(version, description) {
    /**
     * Executes the migration on [connection].
     */
    abstract fun migrate(connection: Connection)
}

/**
 * A migration written with Exposed over R2DBC.
 *
 * [migrate] runs in its own `suspendTransaction` on [DatabaseApi.database], separate from the transaction that writes
 * the history row. If the process stops between the two, the migration runs again on the next start. If it throws,
 * its transaction is rolled back, except for DDL on MariaDB.
 */
abstract class ExposedMigration(version: String, description: String) : SurfMigration(version, description) {
    /**
     * Executes the migration within the given transaction.
     */
    abstract suspend fun R2dbcTransaction.migrate()
}
