package dev.slne.surf.database.migration

import dev.slne.surf.database.migration.internal.FlywayMigrationRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import javax.sql.DataSource

/**
 * Runs Flyway migrations against one database.
 *
 * The database configuration runs every [MigrationSet] bean through it when the context starts. A
 * bean injects the runner to migrate further sets at runtime.
 */
class SurfMigrationRunner internal constructor(
    private val database: R2dbcDatabase,
    migrationDataSource: DataSource,
) {
    private val flyway = FlywayMigrationRunner(database, migrationDataSource)

    /**
     * Runs the migrations of [set] on [Dispatchers.IO], with the set's [MigrationSet.beforeMigrate]
     * and [MigrationSet.afterMigrate] around them.
     *
     * @throws IllegalArgumentException if the set's name or description is invalid
     * @throws IllegalStateException if a check of the set or a migration fails
     */
    suspend fun migrate(set: MigrationSet): MigrationResult {
        set.beforeMigrate(database)

        val migrationPlan = MigrationSpec(set.javaClass.classLoader)
            .apply { with(set) { describe() } }
            .build(set.name)

        val result = migrate(migrationPlan)

        set.afterMigrate(database)

        return result
    }

    /**
     * Runs the migrations described by [block] as the set [name] on [Dispatchers.IO].
     *
     * Each [name] has its own history table `flyway_schema_history_<name>`, so sets sharing a
     * database do not interfere, even with the same versions. Without history, the database is
     * baselined at [MigrationSpec.baselineVersion] (default `"0"`), so a new set still runs its
     * `V1` on a shared, non-empty database. All migrations must therefore be idempotent, for
     * example `CREATE TABLE IF NOT EXISTS`. Cleaning the database is always disabled.
     *
     * SQL files and scanned classes are resolved through the class loader of the caller, unless
     * [MigrationSpec.classLoader] sets another one. [MigrationSpec.VENDOR_PLACEHOLDER] in a SQL
     * location selects the files of the database's vendor.
     *
     * Must not be called inside an open transaction: Flyway uses its own JDBC connection, and on
     * MariaDB the metadata locks of the open transaction can block Flyway's DDL indefinitely.
     *
     * A failed migration throws, naming its version. On MariaDB, which cannot roll back DDL, the
     * migration is recorded as failed in `flyway_schema_history_<name>`, and later runs fail until
     * that row is deleted manually; there is no repair API. On PostgreSQL a failed migration leaves
     * no history row.
     *
     * ```
     * runner.migrate("my_service") {
     *     sql("db/migrations/my_service")
     *     sql("db/migrations/my_service_{vendor}")
     *     classes(V2_Foo)
     *     classNames("dev.slne.surf.myservice.migrations.V3_Seed")
     * }
     * ```
     *
     * @param name lowercased, it must match `[a-z0-9_]{1,40}`
     * @param block at least one of `sql`, `classes` or `classNames` must be given
     * @throws IllegalArgumentException if [name] or [block] is invalid
     * @throws IllegalStateException if a SQL location uses [MigrationSpec.VENDOR_PLACEHOLDER] on a
     * database that is neither MariaDB nor PostgreSQL, a named class is no migration, or a
     * migration fails
     */
    suspend fun migrate(name: String, block: MigrationSpec.() -> Unit): MigrationResult {
        return migrate(MigrationSpec(block.javaClass.classLoader).apply(block).build(name))
    }

    internal suspend fun migrate(plan: MigrationPlan): MigrationResult {
        return withContext(Dispatchers.IO) { flyway.migrate(plan) }
    }
}
