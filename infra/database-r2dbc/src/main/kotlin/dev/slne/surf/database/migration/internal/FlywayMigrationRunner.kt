package dev.slne.surf.database.migration.internal

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.migration.MigrationPlan
import dev.slne.surf.database.migration.MigrationResult
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.postgresql.ds.common.BaseDataSource

/**
 * Runs a [MigrationPlan] with Flyway against the JDBC data source of [api].
 */
internal class FlywayMigrationRunner(private val api: DatabaseApi) {

    /**
     * Builds the Flyway configuration for [plan]: one history table per plugin, baseline on migrate, clean disabled,
     * and only the configured SQL locations (never Flyway's default `db/migration`).
     *
     * @throws IllegalStateException if [api] has no migration data source
     */
    fun configure(plan: MigrationPlan): FluentConfiguration {
        val dataSource = api.migrationDataSource
            ?: error(
                "DatabaseApi has no migrationDataSource, so migrations cannot be run. " +
                    "Create it with DatabaseApi.create(pluginPath, ...) or pass a migrationDataSource."
            )

        val scanned = plan.packages.flatMap { MigrationClassScanner.scan(plan.classLoader, it) }
        val adapters = (plan.migrations + scanned)
            .distinctBy { it.javaClass }
            .map { FlywayJavaMigrationAdapter(api, it) }

        val configuration = Flyway.configure(MigrationClassLoader(plan.classLoader))
            .dataSource(dataSource)
            .table(plan.historyTable)
            .locations(*plan.sqlLocations.toTypedArray())
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
     * @throws IllegalStateException if [api] has no migration data source or a migration fails
     */
    fun migrate(name: String, plan: MigrationPlan): MigrationResult {
        val configuration = configure(plan)
        val result = try {
            configuration.load().migrate()
        } catch (e: FlywayException) {
            throw IllegalStateException("Migration '$name' failed: ${e.message}", e)
        }
        return MigrationResult(
            initialVersion = result.initialSchemaVersion,
            targetVersion = result.targetSchemaVersion,
            migrationsExecuted = result.migrationsExecuted
        )
    }
}
