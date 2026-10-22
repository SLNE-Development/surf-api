package dev.slne.surf.database.migration.internal

import dev.slne.surf.api.core.util.toMutableObjectList
import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.migration.MigrationPlan
import dev.slne.surf.database.migration.MigrationResult
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.flywaydb.core.api.migration.JavaMigration
import org.flywaydb.core.internal.exception.FlywayMigrateException
import org.jetbrains.annotations.Blocking
import org.postgresql.ds.common.BaseDataSource

/**
 * Runs a [MigrationPlan] with Flyway against [DatabaseApi.migrationDataSource].
 */
internal class FlywayMigrationRunner(private val api: DatabaseApi) {

    /**
     * Migrates the database according to [plan]. Blocks the calling thread.
     *
     * @throws IllegalStateException if [configure] fails or a migration fails; the message names the version and
     * description of the failed migration
     */
    @Blocking
    fun migrate(plan: MigrationPlan): MigrationResult {
        val result = try {
            configure(plan).load().migrate()
        } catch (e: FlywayException) {
            throw IllegalStateException(failureMessage(plan.name, e), e)
        }

        return MigrationResult(
            initialVersion = result.initialSchemaVersion,
            targetVersion = result.targetSchemaVersion,
            migrationsExecuted = result.migrationsExecuted
        )
    }

    /**
     * Builds the Flyway configuration for [plan].
     *
     * Only the plan's SQL locations are searched, never Flyway's default `db/migration`. A missing location or a
     * misnamed SQL file fails the run. On PostgreSQL the history table is kept in the schema of the data source.
     *
     * @throws IllegalStateException if [api] has no migration data source, or a scanned package contains no
     * migration classes
     */
    fun configure(plan: MigrationPlan): FluentConfiguration {
        val dataSource = checkNotNull(api.migrationDataSource) {
            "DatabaseApi has no migrationDataSource, so migrations cannot be run. " +
                    "Create it with DatabaseApi.create(pluginPath, ...) or pass a migrationDataSource."
        }

        val configuration = Flyway.configure(MigrationClassLoader(plan.classLoader, plan.sqlPaths))
            .dataSource(dataSource)
            .table(plan.historyTable)
            .locations(*plan.sqlPaths.map { "classpath:$it" }.toTypedArray())
            .failOnMissingLocations(true)
            .validateMigrationNaming(true)
            .javaMigrations(*javaMigrations(plan).toTypedArray())
            .baselineOnMigrate(true)
            .baselineVersion(plan.baselineVersion)
            .cleanDisabled(true)

        val schema = (dataSource as? BaseDataSource)?.currentSchema
        if (!schema.isNullOrBlank()) configuration.defaultSchema(schema)

        return configuration
    }

    /**
     * Returns the listed migrations followed by the scanned ones.
     *
     * A scanned migration is dropped if the same class with the same version is already listed or was found by
     * another package, so overlapping packages register each class once.
     */
    private fun javaMigrations(plan: MigrationPlan): List<JavaMigration> {
        val migrations = plan.migrations.toMutableObjectList()
        val registered = plan.migrations.mapTo(ObjectOpenHashSet()) { it.javaClass to it.version }

        for (packageName in plan.packages) {
            val found = MigrationClassScanner.scan(plan.classLoader, packageName)
            check(found.isNotEmpty()) { "classPackage(\"$packageName\") found no migration classes" }

            for (migration in found) {
                if (registered.add(migration.javaClass to migration.version)) {
                    migrations += migration
                }
            }
        }

        return migrations.map { FlywayJavaMigrationAdapter(api, it) }
    }

    /**
     * Flyway's own message names only SQL files, so the failed migration's version and description are added.
     */
    private fun failureMessage(name: String, e: FlywayException): String {
        val migration = generateSequence<Throwable>(e) { it.cause }
            .filterIsInstance<FlywayMigrateException>()
            .firstNotNullOfOrNull { it.migration }
        val failedAt = migration?.let { " at version ${it.version} (${it.description})" }.orEmpty()

        return "Migration '$name' failed$failedAt: ${e.message}"
    }
}
