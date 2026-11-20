package dev.slne.surf.database.spring

import dev.slne.surf.database.config.ConnectionDetails
import dev.slne.surf.database.config.DatabaseConfig
import dev.slne.surf.database.config.internal.DatabaseConnector
import dev.slne.surf.database.config.internal.PooledConnections
import dev.slne.surf.database.config.internal.newDialect
import dev.slne.surf.database.logger.ComponentSqlLogger
import dev.slne.surf.database.migration.internal.JdbcDataSources
import net.kyori.adventure.text.logger.slf4j.ComponentLogger.logger
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig
import org.jetbrains.exposed.v1.r2dbc.transactions.TransactionManager
import javax.sql.DataSource

/**
 * The pooled database and the JDBC data source of its migrations, owned by one context.
 *
 * [database] is not migrated yet; the context hands it out as a bean only after [SurfMigrationRunner][dev.slne.surf.database.migration.SurfMigrationRunner]
 * ran. [close] unregisters the database from Exposed's [TransactionManager], then disposes the pool and its event
 * loop. The JDBC data source opens a connection per use and holds none.
 */
internal class SurfDatabaseResources private constructor(
    val database: R2dbcDatabase,
    val migrationDataSource: DataSource,
    private val connections: PooledConnections,
) : AutoCloseable {

    @Suppress("ConvertTryFinallyToUseCall")
    override fun close() {
        try {
            TransactionManager.closeAndUnregister(database)
        } finally {
            connections.close()
        }
    }

    companion object {

        /**
         * Opens the pool for [config] under [name] and connects Exposed to it, applying [customize] last.
         *
         * [createMigrationDataSource] runs before anything is allocated or registered, so a failure there leaves no pool
         * and no database in Exposed. If connecting fails, the pool is disposed again.
         */
        fun open(
            config: DatabaseConfig,
            name: String,
            createMigrationDataSource: (ConnectionDetails) -> DataSource = JdbcDataSources::create,
            customize: R2dbcDatabaseConfig.Builder.() -> Unit,
        ): SurfDatabaseResources {
            val migrationDataSource = createMigrationDataSource(config.credentials)
            val connections = PooledConnections.open(config, name)

            try {
                val sqlLogger = ComponentSqlLogger(logger(SQL_LOGGER), config.logLevel)
                val dialect = config.credentials.databaseType.newDialect()
                val database = DatabaseConnector.connect(
                    connections.pool,
                    dialect,
                    sqlLogger,
                    customize,
                )

                return SurfDatabaseResources(database, migrationDataSource, connections)
            } catch (exception: Throwable) {
                connections.close()
                throw exception
            }
        }

        private const val SQL_LOGGER = "dev.slne.surf.database.sql"
    }
}
