package dev.slne.surf.database.migration

import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import java.sql.Connection

/**
 * A code-based migration shipped by a plugin.
 *
 * Extend [JdbcMigration] or [ExposedMigration]; this class is sealed. The [version] and [description] are used to
 * order and label the migration in the plugin's history table, exactly like the `V<version>__<description>.sql`
 * naming of SQL migrations.
 *
 * All migrations must be idempotent (for example `CREATE TABLE IF NOT EXISTS`), because a plugin's history starts at
 * its baseline and the database may already contain objects created before migrations were introduced.
 *
 * @property version Migration version, for example `"2"` or `"2.1"`.
 * @property description Short human-readable description.
 */
sealed class SurfMigration(val version: String, val description: String) {
    /**
     * Optional checksum used to detect changes to an already applied migration, or `null` for none.
     */
    open val checksum: Int? = null
}

/**
 * A migration written against plain JDBC.
 *
 * [migrate] runs inside the JDBC transaction of the migration run. Do not commit or close the [Connection] yourself.
 * If it throws, `migrate()` fails, and what is left behind depends on the database:
 * - PostgreSQL rolls the transaction back, including DDL, and no history row is written.
 * - MariaDB cannot roll back DDL, so Flyway records the migration as failed (`success = false`) in
 *   `flyway_schema_history_<name>`, whatever the migration did. Every later `migrate()` for that plugin fails until
 *   the row is deleted manually; there is no repair API.
 *
 * Must be idempotent.
 */
abstract class JdbcMigration(version: String, description: String) : SurfMigration(version, description) {
    /**
     * Executes the migration on the given [connection].
     */
    abstract fun migrate(connection: Connection)
}

/**
 * A migration written with Exposed over R2DBC.
 *
 * [migrate] runs in a `suspendTransaction` on [dev.slne.surf.database.DatabaseApi.database]. This transaction is
 * separate from the one that records the migration in the history table: if the migration succeeds but the history
 * insert fails (or the process stops in between), the migration runs again on the next start. Therefore it must be
 * idempotent.
 *
 * If [migrate] throws, its own `suspendTransaction` is rolled back (on MariaDB, DDL it already ran stays) and
 * `migrate()` fails. On MariaDB, Flyway also records the migration as failed (`success = false`) in
 * `flyway_schema_history_<name>`, and every later `migrate()` for that plugin fails until the row is deleted manually;
 * there is no repair API. On PostgreSQL no history row is left.
 */
abstract class ExposedMigration(version: String, description: String) : SurfMigration(version, description) {
    /**
     * Executes the migration within the given Exposed transaction.
     */
    abstract suspend fun R2dbcTransaction.migrate()
}
