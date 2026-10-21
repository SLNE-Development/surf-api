package dev.slne.surf.database.migration

import dev.slne.surf.database.config.ConnectionDetails
import dev.slne.surf.database.config.DatabaseType
import dev.slne.surf.database.migration.internal.JdbcDataSources
import dev.slne.surf.database.testing.DatabaseContainers
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertTrue

@Testcontainers(disabledWithoutDocker = true)
class JdbcDataSourcesIT {
    @Test
    fun connectsToMariaDbContainer() {
        val c = DatabaseContainers.mariadb()
        val ds = JdbcDataSources.create(
            ConnectionDetails(
                databaseType = DatabaseType.MARIADB, host = c.host, port = c.getMappedPort(3306),
                database = c.databaseName, username = c.username, password = c.password
            )
        )
        ds.connection.use { assertTrue(it.isValid(5)) }
    }

    @Test
    fun connectsToPostgresContainer() {
        val c = DatabaseContainers.postgres()
        val ds = JdbcDataSources.create(
            ConnectionDetails(
                databaseType = DatabaseType.POSTGRESQL, host = c.host, port = c.getMappedPort(5432),
                database = c.databaseName, username = c.username, password = c.password
            )
        )
        ds.connection.use { assertTrue(it.isValid(5)) }
    }
}
