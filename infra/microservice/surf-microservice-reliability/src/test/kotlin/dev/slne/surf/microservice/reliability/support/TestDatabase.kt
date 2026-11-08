package dev.slne.surf.microservice.reliability.support

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.TestOnlyDatabaseApi
import dev.slne.surf.database.libs.io.r2dbc.pool.ConnectionPool
import dev.slne.surf.database.libs.io.r2dbc.pool.ConnectionPoolConfiguration
import dev.slne.surf.database.libs.io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import dev.slne.surf.database.libs.io.r2dbc.postgresql.PostgresqlConnectionFactory
import dev.slne.surf.database.libs.io.r2dbc.spi.ConnectionFactory
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.vendors.MariaDBDialect
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.database.libs.org.mariadb.r2dbc.MariadbConnectionConfiguration
import dev.slne.surf.database.libs.org.mariadb.r2dbc.MariadbConnectionFactory
import kotlinx.coroutines.flow.single
import org.mariadb.jdbc.MariaDbDataSource
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.JdbcDatabaseContainer
import javax.sql.DataSource

/**
 * One isolated database of a test, a MariaDB database or a PostgreSQL schema named [namespace].
 *
 * Every [open] call stands in for a separate process with its own connection pool. The configuration-file entry point
 * of surf-database needs the relocated configurate of a shadowed microservice, so the tests build the pool directly.
 */
class TestDatabase(
    val vendor: TestVendor,
    private val container: JdbcDatabaseContainer<*>,
    private val username: String,
    private val namespace: String,
) {

    /**
     * Opens a new pool and [DatabaseApi] on this database; [OpenedDatabase.close] releases both.
     */
    @OptIn(TestOnlyDatabaseApi::class)
    fun open(poolName: String): OpenedDatabase {
        val pool = ConnectionPool(
            ConnectionPoolConfiguration.builder()
                .connectionFactory(connectionFactory())
                .name(poolName)
                .maxSize(MAX_POOL_SIZE)
                .build()
        )
        val dialect = when (vendor) {
            TestVendor.POSTGRESQL -> PostgreSQLDialect()
            TestVendor.MARIADB -> MariaDBDialect()
        }
        return OpenedDatabase(DatabaseApi.create(pool, dialect, migrationDataSource()), pool)
    }

    /**
     * Number of rows in `test_entries` with [entry].
     */
    suspend fun entryCount(api: DatabaseApi, entry: String): Long = suspendTransaction(db = api.database) {
        exec("SELECT COUNT(*) FROM test_entries WHERE entry = '$entry'") { row -> (row.get(0) as Number).toLong() }!!
            .single()!!
    }

    private fun connectionFactory(): ConnectionFactory = when (vendor) {
        TestVendor.POSTGRESQL -> PostgresqlConnectionFactory(
            PostgresqlConnectionConfiguration.builder()
                .host(container.host)
                .port(container.firstMappedPort)
                .username(username)
                .password(container.password)
                .database(container.databaseName)
                .schema(namespace)
                .timeZone(SESSION_TIME_ZONE)
                .build()
        )

        TestVendor.MARIADB -> MariadbConnectionFactory.from(
            MariadbConnectionConfiguration.builder()
                .host(container.host)
                .port(container.firstMappedPort)
                .username(username)
                .password(container.password)
                .database(namespace)
                .timezone(SESSION_TIME_ZONE)
                .build()
        )
    }

    private fun migrationDataSource(): DataSource = when (vendor) {
        TestVendor.POSTGRESQL -> PGSimpleDataSource().apply {
            serverNames = arrayOf(container.host)
            portNumbers = intArrayOf(container.firstMappedPort)
            databaseName = container.databaseName
            user = username
            password = container.password
            currentSchema = namespace
            options = "-c TimeZone=$SESSION_TIME_ZONE"
        }

        TestVendor.MARIADB -> MariaDbDataSource(
            "jdbc:mariadb://${container.host}:${container.firstMappedPort}/$namespace" +
                "?connectionTimeZone=$SESSION_TIME_ZONE&forceConnectionTimeZoneToSession=true"
        ).apply {
            user = username
            setPassword(container.password)
        }
    }

    private companion object {
        const val MAX_POOL_SIZE = 8
        const val SESSION_TIME_ZONE = "UTC"
    }
}
