package dev.slne.surf.database

import dev.slne.surf.api.core.util.getCallerClass
import dev.slne.surf.database.config.DatabaseConfig
import dev.slne.surf.database.config.DatabaseType
import dev.slne.surf.database.health.DatabaseHealthRegistry
import dev.slne.surf.database.logger.ComponentSqlLogger
import dev.slne.surf.database.migration.internal.JdbcDataSources
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.IsolationLevel
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import org.jetbrains.exposed.v1.core.vendors.DatabaseDialect
import org.jetbrains.exposed.v1.core.vendors.MariaDBDialect
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig
import org.jetbrains.exposed.v1.r2dbc.transactions.TransactionManager
import org.mariadb.r2dbc.MariadbConnectionConfiguration
import org.mariadb.r2dbc.MariadbConnectionFactory
import org.slf4j.event.Level
import reactor.netty.resources.LoopResources
import java.nio.file.Path
import java.time.Duration.ofMillis
import java.util.concurrent.atomic.AtomicLong
import javax.sql.DataSource

/**
 * Small wrapper around an [R2dbcDatabase] instance.
 *
 * Use [create(Path, String, R2dbcDatabaseConfig.Builder.() -> Unit)] in production to build the connection pool from the
 * on-disk [DatabaseConfig]. A lower-level overload exists mainly for tests.
 */
class DatabaseApi internal constructor(
    val database: R2dbcDatabase,
    healthCheckName: String,
    internal val migrationDataSource: DataSource? = null,
    private val releaseResources: () -> Unit = {}
) {
    init {
        DatabaseHealthRegistry.register(
            databaseApi = this,
            name = healthCheckName
        )
    }

    companion object {
        private val manualDatabaseSequence = AtomicLong()

        /**
         * Creates a [DatabaseApi] using the [DatabaseConfig] located in/relative to [pluginPath].
         *
         * This is the intended production entry point: it reads credentials/pool settings from the config, applies
         * supported `SURF_DATABASE_*` environment-variable overrides, and creates a pooled [ConnectionFactory].
         *
         * @param pluginPath Base path used to locate the database config.
         * @param poolName Optional pool name (defaults to a generated name based on the caller).
         * @param configCustomizer Optional customization hook for Exposed's [R2dbcDatabaseConfig].
         */
        fun create(
            pluginPath: Path,
            fileName: String = "database.yml",
            poolName: String = generatePoolName(),
            configCustomizer: R2dbcDatabaseConfig.Builder.() -> Unit = {}
        ): DatabaseApi {
            val config = DatabaseConfig.create(pluginPath, fileName)
            val databaseType = config.credentials.databaseType
            // Created before any pool or event loop, so an invalid configuration cannot leak them.
            val migrationDataSource = JdbcDataSources.create(config.credentials)
            val loopResources = LoopResources.create(
                "${databaseType.name.lowercase()}-r2dbc-event-loop-$poolName",
                1,
                4,
                true
            )

            val connectionFactory: ConnectionFactory = when (databaseType) {
                DatabaseType.MARIADB -> {
                    val configuration = MariadbConnectionConfiguration.builder()
                        .host(config.credentials.host)
                        .port(config.credentials.resolvedPort)
                        .username(config.credentials.username)
                        .password(config.credentials.password)
                        .database(config.credentials.database)
                        .loopResources(loopResources)
                        .build()

                    MariadbConnectionFactory.from(configuration)
                }

                DatabaseType.POSTGRESQL -> {
                    val configuration = PostgresqlConnectionConfiguration.builder()
                        .host(config.credentials.host)
                        .port(config.credentials.resolvedPort)
                        .username(config.credentials.username)
                        .password(config.credentials.password)
                        .database(config.credentials.database)
                        .schema(config.credentials.schema)
                        .loopResources(loopResources)
                        .build()

                    PostgresqlConnectionFactory(configuration)
                }
            }

            val poolConfig = ConnectionPoolConfiguration.builder()
                .connectionFactory(connectionFactory)
                .acquireRetry(1)
                .initialSize(config.pool.sizing.initialSize)
                .minIdle(config.pool.sizing.minIdle)
                .maxSize(config.pool.sizing.maxSize)
                .maxAcquireTime(ofMillis(config.pool.timeouts.maxAcquireTimeMillis))
                .maxCreateConnectionTime(ofMillis(config.pool.timeouts.maxCreateConnectionTimeMillis))
                .maxIdleTime(ofMillis(config.pool.timeouts.maxIdleTimeMillis))
                .maxLifeTime(ofMillis(config.pool.timeouts.maxLifeTimeMillis))
                .maxValidationTime(ofMillis(config.pool.timeouts.maxValidationTimeMillis))
                .name(poolName)
                .build()


            val pool = ConnectionPool(poolConfig)

            val caller = getCallerClass() ?: DatabaseApi::class.java
            val logger = ComponentLogger.logger(caller)
            val logLevel = config.logLevel

            val dialect = when (databaseType) {
                DatabaseType.MARIADB -> MariaDBDialect()
                DatabaseType.POSTGRESQL -> PostgreSQLDialect()
            }

            return createDatabase(
                connectionFactory = pool,
                dialect = dialect,
                logger = logger,
                logLevel = logLevel,
                healthCheckName = poolName,
                migrationDataSource = migrationDataSource,
                releaseResources = {
                    try {
                        pool.dispose()
                    } finally {
                        loopResources.dispose()
                    }
                },
                configCustomizer = configCustomizer
            )
        }

        /**
         * Creates a [DatabaseApi] from an already constructed [ConnectionFactory].
         *
         * This overload bypasses [DatabaseConfig] loading and pool creation and is therefore mainly intended for tests or
         * special setups where the caller manages the connection factory manually. Prefer
         * [create(Path, String, R2dbcDatabaseConfig.Builder.() -> Unit)] for normal usage.
         *
         * @param connectionFactory The factory to connect with (e.g., testcontainers, in-memory, custom pool).
         * @param dialect The database dialect to use (e.g., MariaDB, Postgres).
         * @param logger Optional logger for Exposed's SQL logging (defaults to a logger named "DatabaseApi").
         * @param logLevel Optional log level for Exposed's SQL logging (
         * @param migrationDataSource Optional JDBC data source used for schema migrations. Without one, migrations
         * cannot be run on the resulting [DatabaseApi].
         * @param configCustomizer Optional customization hook for Exposed's [R2dbcDatabaseConfig].
         */
        @TestOnlyDatabaseApi
        fun create(
            connectionFactory: ConnectionFactory,
            dialect: DatabaseDialect,
            logger: ComponentLogger = ComponentLogger.logger("DatabaseApi"),
            logLevel: Level = Level.DEBUG,
            migrationDataSource: DataSource? = null,
            configCustomizer: R2dbcDatabaseConfig.Builder.() -> Unit = {}
        ): DatabaseApi {
            return createDatabase(
                connectionFactory = connectionFactory,
                dialect = dialect,
                logger = logger,
                logLevel = logLevel,
                healthCheckName = nextManualHealthCheckName(),
                migrationDataSource = migrationDataSource,
                releaseResources = {},
                configCustomizer = configCustomizer
            )
        }

        /**
         * Binary-compatible variant of [create] from before migrations were added; kept so that callers compiled
         * against earlier versions keep working.
         */
        @TestOnlyDatabaseApi
        @Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
        fun create(
            connectionFactory: ConnectionFactory,
            dialect: DatabaseDialect,
            logger: ComponentLogger = ComponentLogger.logger("DatabaseApi"),
            logLevel: Level = Level.DEBUG,
            configCustomizer: R2dbcDatabaseConfig.Builder.() -> Unit = {}
        ): DatabaseApi = create(
            connectionFactory = connectionFactory,
            dialect = dialect,
            logger = logger,
            logLevel = logLevel,
            migrationDataSource = null,
            configCustomizer = configCustomizer
        )

        private fun createDatabase(
            connectionFactory: ConnectionFactory,
            dialect: DatabaseDialect,
            logger: ComponentLogger,
            logLevel: Level,
            healthCheckName: String,
            migrationDataSource: DataSource?,
            releaseResources: () -> Unit,
            configCustomizer: R2dbcDatabaseConfig.Builder.() -> Unit
        ): DatabaseApi {
            val database = R2dbcDatabase.connect(
                connectionFactory,
                R2dbcDatabaseConfig {
                    explicitDialect = dialect
                    sqlLogger = ComponentSqlLogger(logger, logLevel)
                    defaultR2dbcIsolationLevel = IsolationLevel.READ_UNCOMMITTED

                    configCustomizer()
                }
            )

            return DatabaseApi(
                database = database,
                healthCheckName = healthCheckName,
                migrationDataSource = migrationDataSource,
                releaseResources = releaseResources
            )
        }

        private fun generatePoolName(): String {
            val caller = getCallerClass(1)
            val callerName = caller?.simpleName ?: "unknown"
            return "j2bdc-pool-$callerName"
        }

        private fun nextManualHealthCheckName(): String {
            return "database-${manualDatabaseSequence.incrementAndGet()}"
        }
    }

    /**
     * Closes and unregisters this database instance from Exposed's [TransactionManager]. For an instance created
     * from a config file, also disposes the connection pool and its event loop; a [ConnectionFactory] passed in by
     * the caller is left to the caller.
     *
     * Call this during plugin/application shutdown to ensure connections are released.
     */
    fun shutdown() {
        try {
            TransactionManager.closeAndUnregister(database)
        } finally {
            try {
                DatabaseHealthRegistry.unregister(this)
            } finally {
                releaseResources()
            }
        }
    }
}


/**
 * Marks APIs that are primarily intended for tests or special manual setups.
 *
 * The normal code path should use the config-based overloads instead.
 */
@RequiresOptIn(
    message = "This API is intended mainly for tests or special setups. Prefer the config-based create(pluginPath, ...) overload for normal usage.",
    level = RequiresOptIn.Level.WARNING
)
annotation class TestOnlyDatabaseApi
