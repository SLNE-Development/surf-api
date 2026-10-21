package dev.slne.surf.database.migration.internal

import dev.slne.surf.database.config.ConnectionDetails
import dev.slne.surf.database.config.DatabaseType
import org.mariadb.jdbc.MariaDbDataSource
import org.postgresql.ds.PGSimpleDataSource
import javax.sql.DataSource

/**
 * Creates JDBC [DataSource]s for Flyway from [ConnectionDetails].
 *
 * Driver `DataSource` classes are instantiated directly; this code never asks `DriverManager` for a connection itself,
 * because `DriverManager` only hands out drivers visible to the caller's class loader, which an isolated plugin class
 * loader may not see. `MariaDbDataSource` connects through its driver without `DriverManager`. pgjdbc's
 * `BaseDataSource.getConnection` does call `DriverManager.getConnection`, which works because loading the data source
 * registers the (relocated) pgjdbc driver and `DriverManager` checks it against the data source's own class loader,
 * which defined that driver.
 */
internal object JdbcDataSources {
    fun create(details: ConnectionDetails): DataSource = when (details.databaseType) {
        DatabaseType.MARIADB -> MariaDbDataSource(
            "jdbc:mariadb://${details.host}:${details.resolvedPort}/${details.database}"
        ).apply {
            user = details.username
            setPassword(details.password)
        }

        DatabaseType.POSTGRESQL -> PGSimpleDataSource().apply {
            setServerNames(arrayOf(details.host))
            setPortNumbers(intArrayOf(details.resolvedPort))
            databaseName = details.database
            user = details.username
            password = details.password
            currentSchema = details.schema
        }
    }
}
