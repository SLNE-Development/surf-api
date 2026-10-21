package dev.slne.surf.database.migration

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.migration.internal.FlywayMigrationRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.ExperimentalDatabaseMigrationApi
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.migration.r2dbc.MigrationUtils
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import java.nio.file.Path

/**
 * Runs the migrations of one plugin, described by [block], against this database.
 *
 * Each [name] gets its own history table `flyway_schema_history_<name>`, so plugins sharing a database do not
 * interfere with each other, even if they use the same versions. A database without history for [name] is baselined
 * at [MigrationSpec.baselineVersion] (default `"0"`), so a brand-new plugin still runs its `V1` on a shared, non-empty
 * database. All migrations must therefore be idempotent (for example `CREATE TABLE IF NOT EXISTS`). Cleaning the
 * database is always disabled.
 *
 * SQL files and scanned classes are looked up through the class loader of the plugin that calls this function
 * (overridable with [MigrationSpec.classLoader]). Runs on [Dispatchers.IO].
 *
 * Do not call this inside an open transaction: Flyway uses its own JDBC connection, and on MariaDB the metadata locks
 * held by the open transaction can block Flyway's DDL indefinitely.
 *
 * A failed migration throws, naming its version. On MariaDB, which cannot roll back DDL, every failed migration is
 * recorded as failed in `flyway_schema_history_<name>`, and later calls fail until that row is deleted manually; there
 * is no repair API. On PostgreSQL a failed migration leaves no history row.
 *
 * ```
 * databaseApi.migrate("my_plugin") {
 *     sql("db/migrations")
 *     classes(V2_Foo)
 *     classPackage("dev.slne.surf.myplugin.migrations")
 * }
 * ```
 *
 * @param name Plugin name, lowercased; must match `[a-z0-9_]{1,40}`.
 * @param block Describes the migrations; at least one of `sql`, `classes` or `classPackage` must be given.
 * @return the outcome of the run
 * @throws IllegalArgumentException if [name] or [block] is invalid
 * @throws IllegalStateException if this [DatabaseApi] has no JDBC data source for migrations, or a migration fails
 */
suspend fun DatabaseApi.migrate(name: String, block: MigrationSpec.() -> Unit): MigrationResult {
    val plan = MigrationSpec(block.javaClass.classLoader).apply(block).build(name)
    return withContext(Dispatchers.IO) {
        FlywayMigrationRunner(this@migrate).migrate(name, plan)
    }
}

/**
 * Writes the SQL statements that bring this database in line with [tables] to
 * `<scriptDirectory>/<scriptName>.sql`, for use as the next `V<n>__*.sql` migration. This is a development helper;
 * call it against a database that does not yet contain the change.
 *
 * **The generated statements are not idempotent** (for example a plain `CREATE TABLE`, and `DROP` statements for
 * anything that differs). Edit the file before shipping it, since all migrations must be idempotent.
 *
 * @param tables The tables to compare against the database; must not be empty.
 * @param scriptDirectory Directory the script is written to.
 * @param scriptName File name without the `.sql` extension, for example `V2__items`.
 * @return the written file
 * @throws IllegalArgumentException if [tables] is empty
 */
@OptIn(ExperimentalDatabaseMigrationApi::class)
suspend fun DatabaseApi.generateMigrationScript(
    vararg tables: Table,
    scriptDirectory: Path,
    scriptName: String
): Path = suspendTransaction(db = database) {
    MigrationUtils.generateMigrationScript(
        *tables,
        scriptDirectory = scriptDirectory.toString(),
        scriptName = scriptName
    ).toPath()
}
