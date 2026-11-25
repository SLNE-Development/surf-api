package dev.slne.surf.database.migration

import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase

/**
 * A named set of migrations that the database configuration runs before the database is handed out.
 *
 * Every `MigrationSet` bean runs once when the context starts, in `@Order` order, before any bean
 * receives the `R2dbcDatabase`. A set keeps its history in `flyway_schema_history_<name>` and
 * follows the rules of [SurfMigrationRunner.migrate]. SQL locations and scanned packages are
 * resolved through the class loader of the implementing class, unless [MigrationSpec.classLoader]
 * sets another one.
 *
 * A set must not depend on the `R2dbcDatabase` bean, which exists only after every set ran;
 * [beforeMigrate] and [afterMigrate] receive the database instead.
 */
interface MigrationSet {

    /**
     * The name of the set; lowercased, it must match `[a-z0-9_]{1,40}`.
     */
    val name: String

    /**
     * Describes the migrations of the set; at least one of `sql`, `classes` or `classNames` must be
     * given.
     */
    fun MigrationSpec.describe()

    /**
     * Runs before the migrations, for example to check the server version.
     */
    suspend fun beforeMigrate(database: R2dbcDatabase) {}

    /**
     * Runs after the migrations, for example to check the migrated tables.
     */
    suspend fun afterMigrate(database: R2dbcDatabase) {}
}
