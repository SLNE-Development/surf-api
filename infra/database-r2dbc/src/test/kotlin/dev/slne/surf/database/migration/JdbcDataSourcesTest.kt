package dev.slne.surf.database.migration

import dev.slne.surf.database.config.ConnectionDetails
import dev.slne.surf.database.config.DatabaseType
import dev.slne.surf.database.migration.internal.JdbcDataSources
import dev.slne.surf.database.testing.DatabaseContainers
import org.junit.jupiter.api.Test
import org.mariadb.jdbc.MariaDbDataSource
import org.postgresql.ds.PGSimpleDataSource
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JdbcDataSourcesTest {
    @Test
    fun createsMariaDbDataSource() {
        val ds = JdbcDataSources.create(
            ConnectionDetails(
                databaseType = DatabaseType.MARIADB, host = "h", port = 1,
                database = "d", username = "u", password = "p"
            )
        )
        assertIs<MariaDbDataSource>(ds)
        assertTrue(ds.url.contains("jdbc:mariadb://h:1/d"), ds.url)
    }

    @Test
    fun createsPostgresDataSourceWithSchema() {
        val ds = JdbcDataSources.create(
            ConnectionDetails(
                databaseType = DatabaseType.POSTGRESQL, schema = "s", host = "h", port = 1,
                database = "d", username = "u", password = "p"
            )
        )
        assertIs<PGSimpleDataSource>(ds)
        assertContentEquals(arrayOf("h"), ds.serverNames)
        assertContentEquals(intArrayOf(1), ds.portNumbers)
        assertEquals("d", ds.databaseName)
        assertEquals("s", ds.currentSchema)
    }

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
