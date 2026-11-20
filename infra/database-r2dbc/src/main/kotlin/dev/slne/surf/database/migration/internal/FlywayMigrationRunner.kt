package dev.slne.surf.database.migration.internal

import dev.slne.surf.database.config.databaseType
import dev.slne.surf.database.migration.MigrationPlan
import dev.slne.surf.database.migration.MigrationResult
import dev.slne.surf.database.migration.MigrationSpec.Companion.VENDOR_PLACEHOLDER
import dev.slne.surf.database.migration.SurfMigration
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.flywaydb.core.api.migration.JavaMigration
import org.flywaydb.core.internal.exception.FlywayMigrateException
import org.jetbrains.annotations.Blocking
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.postgresql.ds.common.BaseDataSource
import javax.sql.DataSource

/**
 * Runs a [MigrationPlan] with Flyway against [dataSource], a JDBC data source of [database].
 *
 * [ExposedMigration][dev.slne.surf.database.migration.ExposedMigration]s run on [database] itself.
 */
internal class FlywayMigrationRunner(
    private val database: R2dbcDatabase,
    private val dataSource: DataSource,
) {

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
     * Only the plan's SQL locations are searched, never Flyway's default `db/migration`, with
     * [MigrationSpec.VENDOR_PLACEHOLDER] replaced by the vendor of [database]. A missing location or a misnamed SQL file
     * fails the run. On PostgreSQL the history table is kept in the schema of the data source.
     *
     * @throws IllegalStateException if a location uses [MigrationSpec.VENDOR_PLACEHOLDER] on a database that is
     * neither MariaDB nor PostgreSQL, a named class is no migration, or a scanned package contains no migration classes
     */
    fun configure(plan: MigrationPlan): FluentConfiguration {
        val sqlPaths = resolveVendor(plan)
        val configuration = Flyway.configure(MigrationClassLoader(plan.classLoader, sqlPaths))
            .dataSource(dataSource)
            .table(plan.historyTable)
            .locations(*sqlPaths.map { "classpath:$it" }.toTypedArray())
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

    private fun resolveVendor(plan: MigrationPlan): List<String> {
        val vendorPath = plan.sqlPaths.find { VENDOR_PLACEHOLDER in it } ?: return plan.sqlPaths
        val vendor = try {
            database.databaseType.vendor
        } catch (e: IllegalStateException) {
            throw IllegalStateException(
                "Migration '${plan.name}' cannot resolve $VENDOR_PLACEHOLDER in sql(\"$vendorPath\"): ${e.message}",
                e
            )
        }

        return plan.sqlPaths.map { it.replace(VENDOR_PLACEHOLDER, vendor) }
    }

    /**
     * Returns the listed migrations, then the ones named by class, then the scanned ones.
     *
     * A named or scanned migration is dropped if the same class with the same version is already registered, so
     * overlapping entries register each class once.
     */
    private fun javaMigrations(plan: MigrationPlan): List<JavaMigration> {
        val migrations = plan.migrations.toMutableList()
        val registered = plan.migrations.mapTo(HashSet()) { it.javaClass to it.version }

        fun register(candidates: List<SurfMigration>) {
            for (migration in candidates) {
                if (registered.add(migration.javaClass to migration.version)) {
                    migrations += migration
                }
            }
        }

        register(MigrationClassScanner.instantiateNamed(plan.classLoader, plan.classNames))

        for (packageName in plan.packages + plan.optionalPackages) {
            val found = MigrationClassScanner.scan(plan.classLoader, packageName)
            check(found.isNotEmpty() || packageName in plan.optionalPackages) {
                "classPackage(\"$packageName\") found no migration classes"
            }

            register(found)
        }

        return migrations.map { FlywayJavaMigrationAdapter(database, it) }
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
