package dev.slne.surf.database.migration.internal

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.migration.ExposedMigration
import dev.slne.surf.database.migration.JdbcMigration
import dev.slne.surf.database.migration.SurfMigration
import kotlinx.coroutines.runBlocking
import org.flywaydb.core.api.MigrationVersion
import org.flywaydb.core.api.configuration.Configuration
import org.flywaydb.core.api.migration.Context
import org.flywaydb.core.api.migration.JavaMigration
import org.flywaydb.core.api.resolver.ResolvedMigration
import org.flywaydb.core.internal.jdbc.StatementInterceptor
import org.flywaydb.core.internal.resolver.ResolvedMigrationImpl
import org.flywaydb.core.internal.resolver.java.JavaMigrationExecutor
import org.flywaydb.core.internal.util.ClassUtils
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

/**
 * Exposes a [SurfMigration] to Flyway as a [JavaMigration].
 *
 * A [JdbcMigration] runs on Flyway's JDBC connection inside Flyway's transaction. An [ExposedMigration] runs outside
 * of Flyway's transaction, in its own `suspendTransaction` on [DatabaseApi.database].
 */
internal class FlywayJavaMigrationAdapter(
    private val api: DatabaseApi,
    private val migration: SurfMigration,
) : JavaMigration {

    override fun getVersion(): MigrationVersion = MigrationVersion.fromVersion(migration.version)

    override fun getDescription(): String = migration.description

    override fun getChecksum(): Int? = migration.checksum

    override fun canExecuteInTransaction(): Boolean = when (migration) {
        is JdbcMigration -> true
        is ExposedMigration -> false
    }

    /**
     * Same as Flyway's default, but records the class name of the wrapped migration (instead of this adapter) as the
     * script in the history table.
     */
    override fun getResolvedMigration(
        config: Configuration,
        statementInterceptor: StatementInterceptor?
    ): ResolvedMigration = ResolvedMigrationImpl(
        version,
        description,
        migration.javaClass.name,
        checksum,
        null,
        type,
        ClassUtils.getLocationOnDisk(migration.javaClass),
        JavaMigrationExecutor(this, statementInterceptor)
    )

    override fun migrate(context: Context) {
        when (migration) {
            is JdbcMigration -> migration.migrate(context.connection)
            is ExposedMigration -> runBlocking {
                suspendTransaction(db = api.database) {
                    with(migration) { migrate() }
                }
            }
        }
    }
}
