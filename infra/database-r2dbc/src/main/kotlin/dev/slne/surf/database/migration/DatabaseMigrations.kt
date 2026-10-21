package dev.slne.surf.database.migration

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.migration.internal.FlywayMigrationRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
