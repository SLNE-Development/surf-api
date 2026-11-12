package dev.slne.surf.database.testing

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.config.ConnectionDetails
import dev.slne.surf.database.config.DatabaseType
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import io.r2dbc.spi.ConnectionFactory
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import org.jetbrains.exposed.v1.r2dbc.mappers.R2dbcRegistryTypeMapping
import org.mariadb.r2dbc.MariadbConnectionConfiguration
import org.mariadb.r2dbc.MariadbConnectionFactory
import org.slf4j.event.Level

/**
 * Builds a [DatabaseApi] that exchanges time values like version 2.3 of the library.
 *
 * The session time zone is the driver default: the server zone on MariaDB and the JVM default zone at the time of
 * this call on PostgreSQL. The type mapping is Exposed's default, without the library's UTC mapper. Version 2.3's
 * shaded jar fell back to Exposed's hard-coded mapper list, whereas the test class path loads Exposed's service
 * registrations, which add `DateTimeMySqlTypeMapper`; both bind a `java.sql.Timestamp` as its wall-clock time in the
 * JVM default zone.
 */
internal fun ConnectionDetails.toLegacyDatabaseApi(): DatabaseApi = DatabaseApi.createDatabase(
    connectionFactory = legacyConnectionFactory(),
    dialect = dialect(),
    logger = ComponentLogger.logger("LegacyDatabaseApi"),
    logLevel = Level.DEBUG,
    migrationDataSource = null,
    releaseResources = {},
    configCustomizer = { typeMapping = R2dbcRegistryTypeMapping.default() }
)

private fun ConnectionDetails.legacyConnectionFactory(): ConnectionFactory = when (databaseType) {
    DatabaseType.MARIADB -> MariadbConnectionFactory.from(
        MariadbConnectionConfiguration.builder()
            .host(host)
            .port(resolvedPort)
            .username(username)
            .password(password)
            .database(database)
            .build()
    )

    DatabaseType.POSTGRESQL -> PostgresqlConnectionFactory(
        PostgresqlConnectionConfiguration.builder()
            .host(host)
            .port(resolvedPort)
            .username(username)
            .password(password)
            .database(database)
            .schema(schema)
            .build()
    )
}
