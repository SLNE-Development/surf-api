package dev.slne.surf.database.testing

import dev.slne.surf.api.config.type.number.IntOr
import dev.slne.surf.database.config.ConnectionDetails
import dev.slne.surf.database.config.DatabaseType
import dev.slne.surf.database.config.PoolConfig
import dev.slne.surf.database.config.internal.DatabaseConnector
import dev.slne.surf.database.config.internal.R2dbcConnectionFactories
import dev.slne.surf.database.config.internal.newDialect
import dev.slne.surf.database.logger.ComponentSqlLogger
import dev.slne.surf.database.migration.internal.JdbcDataSources
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.Connection
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import org.slf4j.event.Level
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.mariadb.MariaDBContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import reactor.core.publisher.Mono
import java.sql.DriverManager
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

/**
 * Lazily started, JVM-wide singleton database containers shared by all tests.
 */
object DatabaseContainers {
    private val mariadb: MariaDBContainer by lazy { MariaDBContainer("mariadb:12.3").also { it.start() } }
    private val postgres: PostgreSQLContainer by lazy { PostgreSQLContainer("postgres:18").also { it.start() } }
    private val mariadbOutsideUtc: MariaDBContainer by lazy {
        MariaDBContainer("mariadb:12.3").withCommand("--default-time-zone=$SERVER_TIME_ZONE").also { it.start() }
    }
    private val postgresOutsideUtc: PostgreSQLContainer by lazy {
        PostgreSQLContainer("postgres:18")
            .withCommand("postgres", "-c", "fsync=off", "-c", "timezone=$POSTGRES_SERVER_TIME_ZONE")
            .also { it.start() }
    }

    /**
     * Fixed offset of [mariadbOutsideUtc]'s server time zone; differs from UTC and from the JVM zones of the tests.
     */
    const val SERVER_TIME_ZONE = "+09:30"

    /**
     * Server time zone of [postgresOutsideUtc]; differs from UTC and from the JVM zones of the tests.
     */
    const val POSTGRES_SERVER_TIME_ZONE = "Pacific/Chatham"

    fun mariadb(): MariaDBContainer = mariadb
    fun postgres(): PostgreSQLContainer = postgres

    /**
     * MariaDB server whose global time zone is [SERVER_TIME_ZONE].
     */
    fun mariadbOutsideUtc(): MariaDBContainer = mariadbOutsideUtc

    /**
     * PostgreSQL server whose `timezone` setting is [POSTGRES_SERVER_TIME_ZONE].
     */
    fun postgresOutsideUtc(): PostgreSQLContainer = postgresOutsideUtc
}

private val isolationCounter = AtomicInteger()

/**
 * Builds a working [TestDatabase] (R2DBC pool plus JDBC migration data source) for this container.
 *
 * With [fresh] set, a new empty database (MariaDB) or schema (PostgreSQL) is created first, so a test
 * does not see tables of other tests.
 */
fun JdbcDatabaseContainer<*>.toTestDatabase(fresh: Boolean = false): TestDatabase =
    connectionDetails(fresh).toTestDatabase()

/**
 * Connection details for this container's default database (MariaDB) or `public` schema (PostgreSQL).
 *
 * With [fresh] set, they point to a new empty database or schema instead.
 */
internal fun JdbcDatabaseContainer<*>.connectionDetails(fresh: Boolean = false): ConnectionDetails {
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

    return ConnectionDetails(
        databaseType = if (isMariaDb) DatabaseType.MARIADB else DatabaseType.POSTGRESQL,
        schema = schema,
        host = host,
        port = IntOr.Default(if (isMariaDb) getMappedPort(3306) else getMappedPort(5432)),
        database = database,
        username = username,
        password = password
    )
}

/**
 * Builds a working [TestDatabase] (R2DBC pool plus JDBC migration data source) for these details.
 *
 * [sessionSetupSql], if set, runs once on every new R2DBC connection, for example to change a session setting.
 */
internal fun ConnectionDetails.toTestDatabase(sessionSetupSql: String? = null): TestDatabase {
    val pool = ConnectionPool(
        ConnectionPoolConfiguration.builder()
            .connectionFactory(R2dbcConnectionFactories.create(this, Duration.ofMillis(PoolConfig.Timeouts().lockTimeoutMillis)))
            .maxSize(5)
            .apply { if (sessionSetupSql != null) postAllocate { it.executeUpdate(sessionSetupSql) } }
            .build()
    )

    // The test database owns this pool, so close() releases its connections to the shared container.
    return TestDatabase(
        database = DatabaseConnector.connect(
            connectionFactory = pool,
            dialect = databaseType.newDialect(),
            sqlLogger = ComponentSqlLogger(ComponentLogger.logger("TestDatabase"), Level.DEBUG),
        ),
        migrationDataSource = JdbcDataSources.create(this),
        releaseResources = { pool.dispose() },
    )
}

private fun Connection.executeUpdate(sql: String): Mono<Void> =
    Mono.from(createStatement(sql).execute()).flatMap { Mono.from(it.rowsUpdated) }.then()
