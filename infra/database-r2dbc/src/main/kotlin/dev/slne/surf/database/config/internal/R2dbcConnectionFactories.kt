package dev.slne.surf.database.config.internal

import dev.slne.surf.database.config.ConnectionDetails
import dev.slne.surf.database.config.DatabaseType
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import io.r2dbc.spi.ConnectionFactory
import org.mariadb.r2dbc.MariadbConnectionConfiguration
import org.mariadb.r2dbc.MariadbConnectionFactory
import reactor.netty.resources.LoopResources
import java.time.Duration

/**
 * Creates the R2DBC [ConnectionFactory] for [ConnectionDetails].
 *
 * Every connection runs with the session time zone [SESSION_TIME_ZONE], independent of the JVM default zone and the
 * server zone. [JdbcDataSources][dev.slne.surf.database.migration.internal.JdbcDataSources] configures the Flyway
 * connections the same way.
 *
 * Every connection also waits at most the pool's lock timeout for a row lock: MariaDB through
 * `innodb_lock_wait_timeout` in whole seconds, PostgreSQL through `lock_timeout`. The Flyway connections keep the
 * server defaults, so a migration waits for the locks of running transactions.
 */
internal object R2dbcConnectionFactories {
    /**
     * Creates the factory for the vendor of [details].
     *
     * @param loopResources event loop for the driver's network I/O; `null` keeps the driver's shared default
     */
    fun create(
        details: ConnectionDetails,
        lockTimeout: Duration,
        loopResources: LoopResources? = null,
    ): ConnectionFactory = when (details.databaseType) {
        DatabaseType.MARIADB -> mariaDb(details, lockTimeout, loopResources)
        DatabaseType.POSTGRESQL -> postgres(details, lockTimeout, loopResources)
    }

    private fun mariaDb(
        details: ConnectionDetails,
        lockTimeout: Duration,
        loopResources: LoopResources?,
    ): ConnectionFactory {
        val configuration = MariadbConnectionConfiguration.builder()
            .host(details.host)
            .port(details.resolvedPort)
            .username(details.username)
            .password(details.password)
            .database(details.database)
            .timezone(SESSION_TIME_ZONE)
            .sessionVariables(mapOf(MARIADB_LOCK_WAIT_TIMEOUT to lockTimeout.inWholeSecondsRoundedUp()))
            .apply { if (loopResources != null) loopResources(loopResources) }
            .build()

        return MariadbConnectionFactory.from(configuration)
    }

    private fun postgres(
        details: ConnectionDetails,
        lockTimeout: Duration,
        loopResources: LoopResources?,
    ): ConnectionFactory {
        val configuration = PostgresqlConnectionConfiguration.builder()
            .host(details.host)
            .port(details.resolvedPort)
            .username(details.username)
            .password(details.password)
            .database(details.database)
            .schema(details.schema)
            .timeZone(SESSION_TIME_ZONE)
            .lockWaitTimeout(lockTimeout)
            .apply { if (loopResources != null) loopResources(loopResources) }
            .build()

        return PostgresqlConnectionFactory(configuration)
    }

    private const val MARIADB_LOCK_WAIT_TIMEOUT = "innodb_lock_wait_timeout"
}

private const val MILLIS_PER_SECOND = 1000L

/**
 * The duration in whole seconds, rounded up. At most [Int.MAX_VALUE] ms, the pool's validated upper bound, so the
 * result stays far below MariaDB's limit of 100,000,000 s for `innodb_lock_wait_timeout`.
 */
internal fun Duration.inWholeSecondsRoundedUp(): Int = Math.ceilDiv(toMillis(), MILLIS_PER_SECOND).toInt()
