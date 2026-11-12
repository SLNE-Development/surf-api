package dev.slne.surf.database.migration

import dev.slne.surf.api.config.type.number.IntOr
import dev.slne.surf.database.config.ConnectionDetails
import dev.slne.surf.database.config.DatabaseType
import dev.slne.surf.database.migration.internal.JdbcDataSources
import dev.slne.surf.database.testing.DatabaseContainers
import dev.slne.surf.database.testing.withJvmZone
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.mariadb.MariaDBContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Testcontainers(disabledWithoutDocker = true)
class JdbcDataSourcesIT {
    @Test
    fun connectsToMariaDbContainer() {
        mariaDbDataSource(DatabaseContainers.mariadb()).connection.use { assertTrue(it.isValid(5)) }
    }

    @Test
    fun connectsToPostgresContainer() {
        postgresDataSource(DatabaseContainers.postgres()).connection.use { assertTrue(it.isValid(5)) }
    }

    @Test
    fun mariaDbSessionIsUtcOutsideUtc() = withJvmZone(JVM_ZONE) {
        val dataSource = mariaDbDataSource(DatabaseContainers.mariadbOutsideUtc())

        assertEquals("+00:00", dataSource.queryString("SELECT @@session.time_zone"))
    }

    @Test
    fun postgresSessionIsUtcOutsideUtc() = withJvmZone(JVM_ZONE) {
        val dataSource = postgresDataSource(DatabaseContainers.postgresOutsideUtc())

        assertEquals("UTC", dataSource.queryString("SHOW TimeZone"))
    }

    @Test
    fun postgresServerIsOutsideUtc() {
        val c = DatabaseContainers.postgresOutsideUtc()

        // psql sends no TimeZone startup parameter, so it reports the server's default zone.
        val result = c.execInContainer("psql", "-U", c.username, "-d", c.databaseName, "-Atc", "SHOW TimeZone")

        assertEquals(DatabaseContainers.POSTGRES_SERVER_TIME_ZONE, result.stdout.trim())
    }

    private fun mariaDbDataSource(c: MariaDBContainer): DataSource = JdbcDataSources.create(
        ConnectionDetails(
            databaseType = DatabaseType.MARIADB, host = c.host, port = IntOr.Default(c.getMappedPort(3306)),
            database = c.databaseName, username = c.username, password = c.password
        )
    )

    private fun postgresDataSource(c: PostgreSQLContainer): DataSource = JdbcDataSources.create(
        ConnectionDetails(
            databaseType = DatabaseType.POSTGRESQL, host = c.host, port = IntOr.Default(c.getMappedPort(5432)),
            database = c.databaseName, username = c.username, password = c.password
        )
    )

    private fun DataSource.queryString(sql: String): String = connection.use { c ->
        c.createStatement().use { s -> s.executeQuery(sql).use { rows -> rows.next(); rows.getString(1) } }
    }

    private companion object {
        const val JVM_ZONE = "America/New_York"
    }
}
