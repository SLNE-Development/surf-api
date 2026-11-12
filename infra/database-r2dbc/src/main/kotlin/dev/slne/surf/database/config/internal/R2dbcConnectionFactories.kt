package dev.slne.surf.database.config.internal

import dev.slne.surf.database.config.ConnectionDetails
import dev.slne.surf.database.config.DatabaseType
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import io.r2dbc.spi.ConnectionFactory
import org.mariadb.r2dbc.MariadbConnectionConfiguration
import org.mariadb.r2dbc.MariadbConnectionFactory
import reactor.netty.resources.LoopResources

/**
 * Creates the R2DBC [ConnectionFactory] for [ConnectionDetails].
 *
 * Every connection runs with the session time zone [SESSION_TIME_ZONE], independent of the JVM default zone and the
 * server zone. [JdbcDataSources][dev.slne.surf.database.migration.internal.JdbcDataSources] configures the Flyway
 * connections the same way.
 */
internal object R2dbcConnectionFactories {
    /**
     * Creates the factory for the vendor of [details].
     *
     * @param loopResources event loop for the driver's network I/O; `null` keeps the driver's shared default
     */
    fun create(
        details: ConnectionDetails,
        loopResources: LoopResources? = null,
    ): ConnectionFactory = when (details.databaseType) {
        DatabaseType.MARIADB -> mariaDb(details, loopResources)
        DatabaseType.POSTGRESQL -> postgres(details, loopResources)
    }

    private fun mariaDb(
        details: ConnectionDetails,
        loopResources: LoopResources?,
    ): ConnectionFactory {
        val configuration = MariadbConnectionConfiguration.builder()
            .host(details.host)
            .port(details.resolvedPort)
            .username(details.username)
            .password(details.password)
            .database(details.database)
            .timezone(SESSION_TIME_ZONE)
            .apply { if (loopResources != null) loopResources(loopResources) }
            .build()

        return MariadbConnectionFactory.from(configuration)
    }

    private fun postgres(
        details: ConnectionDetails,
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
            .apply { if (loopResources != null) loopResources(loopResources) }
            .build()

        return PostgresqlConnectionFactory(configuration)
    }
}
