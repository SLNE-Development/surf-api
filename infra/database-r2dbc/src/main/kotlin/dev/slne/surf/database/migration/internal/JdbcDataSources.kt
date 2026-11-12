package dev.slne.surf.database.migration.internal

import dev.slne.surf.database.config.ConnectionDetails
import dev.slne.surf.database.config.DatabaseType
import dev.slne.surf.database.config.internal.SESSION_TIME_ZONE
import org.mariadb.jdbc.MariaDbDataSource
import org.postgresql.ds.PGSimpleDataSource
import javax.sql.DataSource

/**
 * Creates the JDBC [DataSource] that Flyway migrates through.
 *
 * The driver's data source is instantiated directly instead of going through `DriverManager`, which only offers
 * drivers visible to the caller's class loader and can miss the relocated drivers from an isolated plugin class
 * loader. pgjdbc still calls `DriverManager` internally; that works because loading [PGSimpleDataSource] registers
 * the driver from the same class loader.
 *
 * Like the R2DBC connections, every JDBC connection runs with the session time zone [SESSION_TIME_ZONE], so SQL
 * migrations see the same `CURRENT_TIMESTAMP` and literal interpretation as the application. Bound parameters still
 * convert through the JVM default zone: `java.sql.Timestamp` and `OffsetDateTime` on MariaDB (Connector/J without
 * `preserveInstants`), `java.sql.Timestamp` for `timestamp` columns on PostgreSQL. A UTC `LocalDateTime` binds exactly.
 */
internal object JdbcDataSources {
    fun create(details: ConnectionDetails): DataSource = when (details.databaseType) {
        DatabaseType.MARIADB -> mariaDb(details)
        DatabaseType.POSTGRESQL -> postgres(details)
    }

    private fun mariaDb(details: ConnectionDetails): MariaDbDataSource {
        val url = buildString {
            append("jdbc:mariadb://")
            append(details.host)
            append(':')
            append(details.resolvedPort)
            append('/')
            append(details.database)
            append("?connectionTimeZone=").append(SESSION_TIME_ZONE)
            append("&forceConnectionTimeZoneToSession=true")
        }

        return MariaDbDataSource(url).apply {
            user = details.username
            setPassword(details.password)
        }
    }

    private fun postgres(details: ConnectionDetails): PGSimpleDataSource = UtcSessionPgDataSource().apply {
        serverNames = arrayOf(details.host)
        portNumbers = intArrayOf(details.resolvedPort)
        databaseName = details.database
        user = details.username
        password = details.password
        currentSchema = details.schema
    }
}
