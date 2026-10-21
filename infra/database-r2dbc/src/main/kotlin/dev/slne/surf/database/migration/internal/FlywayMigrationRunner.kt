package dev.slne.surf.database.migration.internal

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.migration.MigrationPlan
import dev.slne.surf.database.migration.MigrationResult
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.flywaydb.core.internal.exception.FlywayMigrateException
import org.postgresql.ds.common.BaseDataSource

/**
 * Runs a [MigrationPlan] with Flyway against the JDBC data source of [api].
 */
internal class FlywayMigrationRunner(private val api: DatabaseApi) {

    /**
     * Builds the Flyway configuration for [plan]: one history table per plugin, baseline on migrate, clean disabled,
     * and only the configured SQL locations (never Flyway's default `db/migration`). A configured location that cannot
     * be found, or a SQL file in it that does not follow the `V<version>__<description>.sql` naming, fails the run.
     *
     * @throws IllegalStateException if [api] has no migration data source, or a scanned package contains no migration
     * classes
     */
    fun configure(plan: MigrationPlan): FluentConfiguration {
        val dataSource = api.migrationDataSource
            ?: error(
                "DatabaseApi has no migrationDataSource, so migrations cannot be run. " +
                    "Create it with DatabaseApi.create(pluginPath, ...) or pass a migrationDataSource."
            )

        // A class can be found by several (nested) packages, or be listed as well; a listed instance replaces the
        // scanned one only if it has the same version.
        val listed = plan.migrations.mapTo(HashSet()) { it.javaClass to it.version }
        val scanned = plan.packages
            .flatMap { packageName ->
                MigrationClassScanner.scan(plan.classLoader, packageName).ifEmpty {
                    error("classPackage(\"$packageName\") found no migration classes")
                }
            }
            .distinctBy { it.javaClass }
            .filterNot { (it.javaClass to it.version) in listed }
        val adapters = (plan.migrations + scanned).map { FlywayJavaMigrationAdapter(api, it) }

        val configuration = Flyway.configure(MigrationClassLoader(plan.classLoader))
            .dataSource(dataSource)
            .table(plan.historyTable)
            .locations(*plan.sqlLocations.toTypedArray())
            .failOnMissingLocations(true)
            .validateMigrationNaming(true)
            .javaMigrations(*adapters.toTypedArray())
            .baselineOnMigrate(true)
            .baselineVersion(plan.baselineVersion)
            .cleanDisabled(true)

        // PostgreSQL: keep the history table in the schema the DatabaseApi works in.
        val schema = (dataSource as? BaseDataSource)?.currentSchema
        if (!schema.isNullOrBlank()) configuration.defaultSchema(schema)

        return configuration
    }

    /**
     * Migrates the database according to [plan]. Blocks the calling thread.
     *
     * @throws IllegalStateException if [api] has no migration data source or a migration fails; the message names the
     * version and description of the failed migration
     */
    fun migrate(name: String, plan: MigrationPlan): MigrationResult {
        val configuration = configure(plan)
        val result = try {
            configuration.load().migrate()
        } catch (e: FlywayException) {
            throw IllegalStateException("Migration '$name' failed${failedMigration(e)}: ${e.message}", e)
        }
        return MigrationResult(
            initialVersion = result.initialSchemaVersion,
            targetVersion = result.targetSchemaVersion,
            migrationsExecuted = result.migrationsExecuted
        )
    }

    /**
     * Names the migration that failed, for example ` at version 2 (backfill items)`, or returns an empty string if
     * [e] does not belong to a single migration. Flyway's own message only names SQL files, not code migrations.
     */
    private fun failedMigration(e: FlywayException): String {
        val migration = generateSequence<Throwable>(e) { it.cause }
            .filterIsInstance<FlywayMigrateException>()
            .firstNotNullOfOrNull { it.migration }
            ?: return ""
        return " at version ${migration.version} (${migration.description})"
    }
}
