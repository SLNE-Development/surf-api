package dev.slne.surf.database

import dev.slne.surf.api.common.util.getCallerClass
import dev.slne.surf.database.columns.time.internal.UtcSessionTypeMapper
import dev.slne.surf.database.config.DatabaseConfig
import dev.slne.surf.database.config.DatabaseType
import dev.slne.surf.database.config.internal.R2dbcConnectionFactories
import dev.slne.surf.database.logger.ComponentSqlLogger
import dev.slne.surf.database.migration.internal.JdbcDataSources
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.IsolationLevel
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import org.jetbrains.exposed.v1.core.vendors.DatabaseDialect
import org.jetbrains.exposed.v1.core.vendors.MariaDBDialect
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig
import org.jetbrains.exposed.v1.r2dbc.mappers.R2dbcRegistryTypeMapping
import org.jetbrains.exposed.v1.r2dbc.mappers.R2dbcTypeMapping
import org.jetbrains.exposed.v1.r2dbc.transactions.TransactionManager
import org.slf4j.event.Level
import reactor.netty.resources.LoopResources
import java.nio.file.Path
import java.time.Duration.ofMillis
import javax.sql.DataSource

/**
 * Small wrapper around an [R2dbcDatabase] instance.
 *
 * Use [create(Path, String, R2dbcDatabaseConfig.Builder.() -> Unit)] in production to build the connection pool from the
 * on-disk [DatabaseConfig]. A lower-level overload exists mainly for tests.
 */
class DatabaseApi internal constructor(
    val database: R2dbcDatabase,
    private val dialect: DatabaseDialect,
    internal val migrationDataSource: DataSource? = null,
    private val releaseResources: () -> Unit = {},
    /**
     * Maximum number of connections in the pool that this instance created from its config, or `null` for an
     * instance built on a caller-supplied [ConnectionFactory].
     */
    val poolMaxSize: Int? = null,
) {

    /**
     * Vendor of [database], derived from its dialect.
     *
     * @throws IllegalStateException if the dialect is neither MariaDB nor PostgreSQL, which only an instance from
     * the manual `create(connectionFactory, dialect, ...)` overloads can have
     */
    val databaseType: DatabaseType by lazy {
        when (dialect) {
            is MariaDBDialect -> DatabaseType.MARIADB
            is PostgreSQLDialect -> DatabaseType.POSTGRESQL
            else -> error(
                "Dialect ${dialect.javaClass.simpleName} has no DatabaseType, " +
                        "expected one of ${DatabaseType.entries.joinToString()}"
            )
        }
    }

    companion object {
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
            val migrationDataSource = JdbcDataSources.create(config.credentials)
            val loopResources = LoopResources.create(
                "${databaseType.vendor}-r2dbc-event-loop-$poolName",
                1,
                4,
                true
            )

            val connectionFactory = R2dbcConnectionFactories.create(config.credentials, loopResources)

            val poolConfig = ConnectionPoolConfiguration.builder()
                .connectionFactory(connectionFactory)
                // A retry would restart the acquire timeout at the end of the queue
                .acquireRetry(0)
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
                migrationDataSource = migrationDataSource,
                releaseResources = {
                    try {
                        pool.dispose()
                    } finally {
                        loopResources.dispose()
                    }
                },
                configCustomizer = configCustomizer,
                poolMaxSize = config.pool.sizing.maxSize
            )
        }

        /**
         * Creates a [DatabaseApi] from an already constructed [ConnectionFactory]. Migrations cannot be run on the
         * result; use the overload with a `migrationDataSource` for that.
         *
         * This overload bypasses [DatabaseConfig] loading and pool creation and is therefore mainly intended for tests or
         * special setups where the caller manages the connection factory manually. Prefer
         * [create(Path, String, R2dbcDatabaseConfig.Builder.() -> Unit)] for normal usage.
         *
         * The time column types, the library's and Exposed's, expect the session time zone UTC, which the config-based
         * overload sets on all its connections. A caller-built [connectionFactory] must set it too (builder option
         * `timezone("UTC")` for MariaDB, `timeZone("UTC")` for PostgreSQL); otherwise instants and `CURRENT_TIMESTAMP`
         * defaults shift by the offset of the session zone.
         *
         * @param connectionFactory The factory to connect with (e.g., testcontainers, in-memory, custom pool).
         * @param dialect The database dialect to use (e.g., MariaDB, Postgres).
         * @param logger Optional logger for Exposed's SQL logging (defaults to a logger named "DatabaseApi").
         * @param logLevel Optional log level for Exposed's SQL logging (
         * @param configCustomizer Optional customization hook for Exposed's [R2dbcDatabaseConfig].
         */
        @TestOnlyDatabaseApi
        fun create(
            connectionFactory: ConnectionFactory,
            dialect: DatabaseDialect,
            logger: ComponentLogger = ComponentLogger.logger("DatabaseApi"),
            logLevel: Level = Level.DEBUG,
            configCustomizer: R2dbcDatabaseConfig.Builder.() -> Unit = {}
        ): DatabaseApi = createManual(connectionFactory, dialect, logger, logLevel, null, configCustomizer)

        /**
         * Creates a [DatabaseApi] from an already constructed [ConnectionFactory] that can run migrations through
         * [migrationDataSource].
         *
         * Like the overload without a data source, this is mainly intended for tests or special setups; the caller
         * keeps ownership of [connectionFactory] and [migrationDataSource], and must configure the session time zone
         * UTC on both as described there; SQL migrations otherwise evaluate `CURRENT_TIMESTAMP` and time literals in
         * another zone than the application.
         *
         * @param connectionFactory The factory to connect with (e.g., testcontainers, in-memory, custom pool).
         * @param dialect The database dialect to use (e.g., MariaDB, Postgres).
         * @param migrationDataSource JDBC data source used for schema migrations; it must point at the same database.
         * @param logger Optional logger for Exposed's SQL logging (defaults to a logger named "DatabaseApi").
         * @param logLevel Optional log level for Exposed's SQL logging.
         * @param configCustomizer Optional customization hook for Exposed's [R2dbcDatabaseConfig].
         */
        @TestOnlyDatabaseApi
        fun create(
            connectionFactory: ConnectionFactory,
            dialect: DatabaseDialect,
            migrationDataSource: DataSource,
            logger: ComponentLogger = ComponentLogger.logger("DatabaseApi"),
            logLevel: Level = Level.DEBUG,
            configCustomizer: R2dbcDatabaseConfig.Builder.() -> Unit = {}
        ): DatabaseApi = createManual(connectionFactory, dialect, logger, logLevel, migrationDataSource, configCustomizer)

        private fun createManual(
            connectionFactory: ConnectionFactory,
            dialect: DatabaseDialect,
            logger: ComponentLogger,
            logLevel: Level,
            migrationDataSource: DataSource?,
            configCustomizer: R2dbcDatabaseConfig.Builder.() -> Unit
        ): DatabaseApi = createDatabase(
            connectionFactory = connectionFactory,
            dialect = dialect,
            logger = logger,
            logLevel = logLevel,
            migrationDataSource = migrationDataSource,
            releaseResources = {},
            configCustomizer = configCustomizer
        )

        /**
         * Builds the [DatabaseApi]; [releaseResources] runs on [shutdown] after the database is unregistered.
         *
         * Transactions default to READ COMMITTED, so they never read uncommitted rows of other transactions; a
         * [configCustomizer] may set another `defaultR2dbcIsolationLevel`. Exposed applies the level per transaction,
         * so MariaDB's session variable `@@tx_isolation` does not show it.
         *
         * The type mapping registers [UtcSessionTypeMapper] on top of Exposed's defaults. A [configCustomizer]
         * that replaces `typeMapping` drops it, and Exposed's `timestamp()` columns then convert through the JVM
         * default zone again.
         */
        internal fun createDatabase(
            connectionFactory: ConnectionFactory,
            dialect: DatabaseDialect,
            logger: ComponentLogger,
            logLevel: Level,
            migrationDataSource: DataSource?,
            releaseResources: () -> Unit,
            configCustomizer: R2dbcDatabaseConfig.Builder.() -> Unit,
            poolMaxSize: Int? = null
        ): DatabaseApi {
            val database = R2dbcDatabase.connect(
                connectionFactory,
                R2dbcDatabaseConfig {
                    explicitDialect = dialect
                    typeMapping = utcTypeMapping()
                    sqlLogger = ComponentSqlLogger(logger, logLevel)
                    defaultR2dbcIsolationLevel = IsolationLevel.READ_COMMITTED

                    configCustomizer()
                }
            )

            return DatabaseApi(
                database = database,
                dialect = dialect,
                migrationDataSource = migrationDataSource,
                releaseResources = releaseResources,
                poolMaxSize = poolMaxSize
            )
        }

        private fun utcTypeMapping(): R2dbcTypeMapping {
            val mapping = R2dbcRegistryTypeMapping.default() as R2dbcRegistryTypeMapping
            mapping.register(UtcSessionTypeMapper)
            return mapping
        }

        private fun generatePoolName(): String {
            val caller = getCallerClass(1)
            val callerName = caller?.simpleName ?: "unknown"
            return "j2bdc-pool-$callerName"
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
            releaseResources()
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
