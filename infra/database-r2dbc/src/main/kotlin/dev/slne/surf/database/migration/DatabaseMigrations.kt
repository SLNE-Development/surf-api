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
 * Runs the migrations of one plugin, described by [block], against this database on [Dispatchers.IO].
 *
 * Each [name] has its own history table `flyway_schema_history_<name>`, so plugins sharing a database do not
 * interfere, even with the same versions. Without history, the database is baselined at
 * [MigrationSpec.baselineVersion] (default `"0"`), so a new plugin still runs its `V1` on a shared, non-empty
 * database. All migrations must therefore be idempotent, for example `CREATE TABLE IF NOT EXISTS`. Cleaning the
 * database is always disabled.
 *
 * SQL files and scanned classes are resolved through the class loader of the calling plugin, unless
 * [MigrationSpec.classLoader] sets another one.
 *
 * Must not be called inside an open transaction: Flyway uses its own JDBC connection, and on MariaDB the metadata
 * locks of the open transaction can block Flyway's DDL indefinitely.
 *
 * A failed migration throws, naming its version. On MariaDB, which cannot roll back DDL, the migration is recorded as
 * failed in `flyway_schema_history_<name>`, and later calls fail until that row is deleted manually; there is no
 * repair API. On PostgreSQL a failed migration leaves no history row.
 *
 * ```
 * databaseApi.migrate("my_plugin") {
 *     sql("db/migrations/my_plugin")
 *     classes(V2_Foo)
 *     classPackage("dev.slne.surf.myplugin.migrations")
 * }
 * ```
 *
 * @param name Plugin name; lowercased, it must match `[a-z0-9_]{1,40}`.
 * @param block Describes the migrations; at least one of `sql`, `classes` or `classPackage` must be given.
 * @throws IllegalArgumentException if [name] or [block] is invalid
 * @throws IllegalStateException if this [DatabaseApi] has no migration data source, a scanned package contains no
 * migration classes, or a migration fails
 */
suspend fun DatabaseApi.migrate(name: String, block: MigrationSpec.() -> Unit): MigrationResult {
    val plan = MigrationSpec(block.javaClass.classLoader).apply(block).build(name)
    val runner = FlywayMigrationRunner(this)

    return withContext(Dispatchers.IO) { runner.migrate(plan) }
}

/**
 * Writes the statements that bring this database in line with [tables] to `<scriptDirectory>/<scriptName>.sql`, as a
 * starting point for the next `V<n>__*.sql` migration.
 *
 * Development helper: call it against a database that does not contain the change yet. **The generated statements
 * are not idempotent** (plain `CREATE TABLE`, `DROP` for anything that differs), so edit the file before shipping it.
 *
 * @param scriptName File name without the `.sql` extension, for example `V2__items`.
 * @return the written file
 * @throws IllegalArgumentException if [tables] is empty
 */
@OptIn(ExperimentalDatabaseMigrationApi::class)
suspend fun DatabaseApi.generateMigrationScript(
    vararg tables: Table,
    scriptDirectory: Path,
    scriptName: String,
): Path = suspendTransaction(db = database) {
    MigrationUtils.generateMigrationScript(
        *tables,
        scriptDirectory = scriptDirectory.toString(),
        scriptName = scriptName,
    ).toPath()
}
