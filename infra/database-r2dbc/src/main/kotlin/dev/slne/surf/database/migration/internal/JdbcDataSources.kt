package dev.slne.surf.database.migration.internal

import dev.slne.surf.database.config.ConnectionDetails
import dev.slne.surf.database.config.DatabaseType
import org.mariadb.jdbc.MariaDbDataSource
import org.postgresql.ds.PGSimpleDataSource
import javax.sql.DataSource

/**
 * Creates JDBC [DataSource]s for Flyway from [ConnectionDetails].
 *
 * Driver `DataSource` classes are instantiated directly instead of going through `DriverManager`, because
 * `DriverManager` does not see drivers that were loaded by an isolated plugin classloader.
 */
internal object JdbcDataSources {
    fun create(details: ConnectionDetails): DataSource = when (details.databaseType) {
        DatabaseType.MARIADB -> MariaDbDataSource(
            "jdbc:mariadb://${details.host}:${details.port}/${details.database}"
        ).apply {
            user = details.username
            setPassword(details.password)
        }

        DatabaseType.POSTGRESQL -> PGSimpleDataSource().apply {
            setServerNames(arrayOf(details.host))
            setPortNumbers(intArrayOf(details.port))
            databaseName = details.database
            user = details.username
            password = details.password
            currentSchema = details.schema
        }
    }
}
