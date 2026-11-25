package dev.slne.surf.microservice.reliability.support

import dev.slne.surf.api.config.type.number.IntOr
import dev.slne.surf.database.config.ConnectionDetails
import dev.slne.surf.database.config.DatabaseConfig
import dev.slne.surf.database.config.DatabaseType
import dev.slne.surf.database.config.PoolConfig
import dev.slne.surf.database.reliability.ReliabilityMigrationSet
import dev.slne.surf.database.spring.SurfDatabaseAutoConfiguration
import kotlinx.coroutines.flow.single
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.support.registerBean
import org.springframework.core.env.MapPropertySource
import org.testcontainers.containers.JdbcDatabaseContainer

/**
 * One isolated database of a test, a MariaDB database or a PostgreSQL schema named [namespace].
 *
 * Every [open] call stands in for a separate process: a Spring context with its own connection
 * pool, started with [SurfDatabaseAutoConfiguration] like the microservice runtime does.
 */
class TestDatabase(
    val vendor: TestVendor,
    private val container: JdbcDatabaseContainer<*>,
    private val username: String,
    private val namespace: String,
) {

    /**
     * The connection settings of this database.
     */
    fun connectionDetails(): ConnectionDetails = when (vendor) {
        TestVendor.POSTGRESQL -> ConnectionDetails(
            databaseType = DatabaseType.POSTGRESQL,
            schema = namespace,
            host = container.host,
            port = IntOr.Default(container.firstMappedPort),
            database = container.databaseName,
            username = username,
            password = container.password,
        )

        TestVendor.MARIADB -> ConnectionDetails(
            databaseType = DatabaseType.MARIADB,
            host = container.host,
            port = IntOr.Default(container.firstMappedPort),
            database = namespace,
            username = username,
            password = container.password,
        )
    }

    /**
     * Starts a context on this database, in which [configure] registers the beans the context needs
     * besides the database configuration. They are registered before
     * [SurfDatabaseAutoConfiguration], so its `@ConditionalOnMissingBean` back-offs see them.
     * [OpenedDatabase.close] releases the pool.
     *
     * @param poolName the name of the connection pool
     */
    fun open(
        poolName: String,
        configure: AnnotationConfigApplicationContext.() -> Unit = {},
    ): OpenedDatabase {
        val context = AnnotationConfigApplicationContext()

        try {
            context.environment.propertySources.addFirst(
                MapPropertySource(
                    "test",
                    mapOf(APPLICATION_NAME to poolName),
                ),
            )
            val details = connectionDetails()
            context.registerBean<DatabaseConfig> {
                DatabaseConfig(
                    credentials = details,
                    pool = POOL,
                )
            }
            context.configure()
            context.register(SurfDatabaseAutoConfiguration::class.java)
            context.refresh()

            return OpenedDatabase(context)
        } catch (e: Throwable) {
            context.close()
            throw e
        }
    }

    /**
     * Like [open], with a [ReliabilityMigrationSet] bean that creates the stored reply table, for
     * tests that build their own `AtomicOperations`.
     */
    fun openWithReliabilitySchema(poolName: String): OpenedDatabase {
        return open(poolName) { registerBean<ReliabilityMigrationSet>() }
    }

    /**
     * Number of rows in `test_entries` with [entry].
     */
    suspend fun entryCount(
        database: R2dbcDatabase,
        entry: String,
    ): Long = suspendTransaction(db = database) {
        exec(
            "SELECT COUNT(*) FROM test_entries WHERE entry = '$entry'",
        ) { row -> (row.get(0) as Number).toLong() }!!
            .single()!!
    }

    private companion object {
        const val APPLICATION_NAME = "spring.application.name"
        val POOL = PoolConfig(sizing = PoolConfig.Sizing(initialSize = 1, maxSize = 8))
    }
}
