package dev.slne.surf.database.testing

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.config.ConnectionDetails
import dev.slne.surf.database.config.DatabaseType
import dev.slne.surf.database.migration.internal.JdbcDataSources
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import io.r2dbc.spi.ConnectionFactory
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import org.jetbrains.exposed.v1.core.vendors.MariaDBDialect
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.mariadb.r2dbc.MariadbConnectionConfiguration
import org.mariadb.r2dbc.MariadbConnectionFactory
import org.slf4j.event.Level
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.mariadb.MariaDBContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import java.sql.DriverManager
import java.util.concurrent.atomic.AtomicInteger

/**
 * Lazily started, JVM-wide singleton database containers shared by all tests.
 */
object DatabaseContainers {
    private val mariadb: MariaDBContainer by lazy { MariaDBContainer("mariadb:11").also { it.start() } }
    private val postgres: PostgreSQLContainer by lazy { PostgreSQLContainer("postgres:17").also { it.start() } }

    fun mariadb(): MariaDBContainer = mariadb
    fun postgres(): PostgreSQLContainer = postgres
}

private val isolationCounter = AtomicInteger()

/**
 * Builds a working [DatabaseApi] (R2DBC pool plus JDBC migration data source) for this container.
 *
 * With [fresh] set, a new empty database (MariaDB) or schema (PostgreSQL) is created first, so a test
 * does not see tables of other tests.
 */
fun JdbcDatabaseContainer<*>.toDatabaseApi(fresh: Boolean = false): DatabaseApi {
    val isMariaDb = this is MariaDBContainer
    val unique = "t${isolationCounter.incrementAndGet()}_${System.nanoTime()}"

    // MariaDB: root is required to create databases and the test user is only granted on the default one.
    val username = if (isMariaDb) "root" else username
    var database = databaseName
    var schema = "public"

    if (fresh) {
        DriverManager.getConnection(jdbcUrl, username, password).use { connection ->
            connection.createStatement().use { statement ->
                if (isMariaDb) {
                    database = "db_$unique"
                    statement.execute("CREATE DATABASE `$database`")
                } else {
                    schema = "s_$unique"
                    statement.execute("CREATE SCHEMA \"$schema\"")
                }
            }
        }
    }

    val details = ConnectionDetails(
        databaseType = if (isMariaDb) DatabaseType.MARIADB else DatabaseType.POSTGRESQL,
        schema = schema,
        host = host,
        port = if (isMariaDb) getMappedPort(3306) else getMappedPort(5432),
        database = database,
        username = username,
        password = password
    )

    val connectionFactory: ConnectionFactory = if (isMariaDb) {
        MariadbConnectionFactory.from(
            MariadbConnectionConfiguration.builder()
                .host(details.host).port(details.resolvedPort)
                .username(details.username).password(details.password)
                .database(details.database)
                .build()
        )
    } else {
        PostgresqlConnectionFactory(
            PostgresqlConnectionConfiguration.builder()
                .host(details.host).port(details.resolvedPort)
                .username(details.username).password(details.password)
                .database(details.database).schema(details.schema)
                .build()
        )
    }

    val pool = ConnectionPool(
        ConnectionPoolConfiguration.builder().connectionFactory(connectionFactory).maxSize(5).build()
    )

    // The API owns this pool, so shutdown() releases its connections to the shared container.
    return DatabaseApi.createDatabase(
        connectionFactory = pool,
        dialect = if (isMariaDb) MariaDBDialect() else PostgreSQLDialect(),
        logger = ComponentLogger.logger("DatabaseApi"),
        logLevel = Level.DEBUG,
        healthCheckName = "test-$unique",
        migrationDataSource = JdbcDataSources.create(details),
        releaseResources = { pool.dispose() },
        configCustomizer = {}
    )
}
