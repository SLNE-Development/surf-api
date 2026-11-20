package dev.slne.surf.database.testing

import dev.slne.surf.database.migration.MigrationResult
import dev.slne.surf.database.migration.MigrationSpec
import dev.slne.surf.database.migration.SurfMigrationRunner
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.transactions.TransactionManager
import javax.sql.DataSource

/**
 * An [R2dbcDatabase] with the JDBC data source and the [SurfMigrationRunner] of its migrations, built from the
 * module's internal factories instead of the Spring auto-configuration so a test controls pool size, session setup and
 * type mapping.
 *
 * [close] unregisters the database from Exposed and runs [releaseResources], which disposes whatever pool the test
 * created.
 */
class TestDatabase(
    val database: R2dbcDatabase,
    val migrationDataSource: DataSource,
    private val releaseResources: () -> Unit,
) : AutoCloseable {

    val migrationRunner = SurfMigrationRunner(database, migrationDataSource)

    /**
     * Runs the migrations described by [block] as the set [name], like [SurfMigrationRunner.migrate].
     */
    suspend fun migrate(name: String, block: MigrationSpec.() -> Unit): MigrationResult =
        migrationRunner.migrate(name, block)

    override fun close() {
        try {
            TransactionManager.closeAndUnregister(database)
        } finally {
            releaseResources()
        }
    }
}
