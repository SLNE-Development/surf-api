package dev.slne.database

import dev.slne.database.config.DatabaseConfig
import dev.slne.surf.surfapi.core.api.util.getCallerClass
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import io.r2dbc.spi.ConnectionFactoryOptions.*
import org.jetbrains.exposed.v1.core.vendors.MariaDBDialect
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig
import org.jetbrains.exposed.v1.r2dbc.transactions.TransactionManager
import java.nio.file.Path
import java.time.Duration.ofMillis

/**
 * Small wrapper around an [R2dbcDatabase] instance.
 *
 * Use [create(Path, String, R2dbcDatabaseConfig.Builder.() -> Unit)] in production to build the connection pool from the
 * on-disk [DatabaseConfig]. A lower-level overload exists mainly for tests.
 */
class DatabaseApi internal constructor(val database: R2dbcDatabase) {

    companion object {
        /**
         * Creates a [DatabaseApi] using the [DatabaseConfig] located in/relative to [pluginPath].
         *
         * This is the intended production entry point: it reads credentials/pool settings from the config and creates a
         * pooled [ConnectionFactory].
         *
         * @param pluginPath Base path used to locate the database config.
         * @param poolName Optional pool name (defaults to a generated name based on the caller).
         * @param configCustomizer Optional customization hook for Exposed's [R2dbcDatabaseConfig].
         */
        @OptIn(TestOnlyDatabaseApi::class)
        fun create(
            pluginPath: Path,
            poolName: String = generatePoolName(),
            configCustomizer: R2dbcDatabaseConfig.Builder.() -> Unit = {}
        ): DatabaseApi {
            val config = DatabaseConfig.create(pluginPath)

            val connectionFactoryOptions = ConnectionFactoryOptions.builder().apply {
                option(DRIVER, "pool")
                option(PROTOCOL, "mariadb")
                option(HOST, config.credentials.host)
                option(PORT, config.credentials.port)
                option(USER, config.credentials.username)
                option(PASSWORD, config.credentials.password)
                option(DATABASE, config.credentials.database)
            }.build()

            val poolConfig = ConnectionPoolConfiguration.builder()
                .connectionFactory(ConnectionFactories.get(connectionFactoryOptions))
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
            return create(pool, configCustomizer)
        }

        /**
         * Creates a [DatabaseApi] from an already constructed [ConnectionFactory].
         *
         * This overload bypasses [DatabaseConfig] loading and pool creation and is therefore mainly intended for tests or
         * special setups where the caller manages the connection factory manually. Prefer
         * [create(Path, String, R2dbcDatabaseConfig.Builder.() -> Unit)] for normal usage.
         *
         * @param connectionFactory The factory to connect with (e.g., testcontainers, in-memory, custom pool).
         * @param configCustomizer Optional customization hook for Exposed's [R2dbcDatabaseConfig].
         */
        @TestOnlyDatabaseApi
        fun create(
            connectionFactory: ConnectionFactory,
            configCustomizer: R2dbcDatabaseConfig.Builder.() -> Unit = {}
        ): DatabaseApi {
            val database = R2dbcDatabase.connect(connectionFactory, R2dbcDatabaseConfig {
                explicitDialect = MariaDBDialect()
                configCustomizer()
            })

            return DatabaseApi(database)
        }

        private fun generatePoolName(): String {
            val caller = getCallerClass(1)
            val callerName = caller?.simpleName ?: "unknown"
            return "j2bdc-pool-$callerName"
        }
    }

    /**
     * Closes and unregisters this database instance from Exposed's [TransactionManager].
     *
     * Call this during plugin/application shutdown to ensure connections are released.
     */
    fun shutdown() {
        TransactionManager.closeAndUnregister(database)
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