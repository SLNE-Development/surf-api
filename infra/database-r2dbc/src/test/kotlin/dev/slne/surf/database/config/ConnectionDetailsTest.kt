package dev.slne.surf.database.config

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class ConnectionDetailsTest {

    private fun DatabaseConfig.withEnvironment(vararg variables: Pair<String, String>) =
        withEnvironmentOverrides { mapOf(*variables)[it] }

    @Test
    fun defaultPortDependsOnTheDatabaseType() {
        assertEquals(3306, ConnectionDetails(databaseType = DatabaseType.MARIADB).resolvedPort)
        assertEquals(5432, ConnectionDetails(databaseType = DatabaseType.POSTGRESQL).resolvedPort)
    }

    @Test
    fun configuredPortWins() {
        assertEquals(6000, ConnectionDetails(databaseType = DatabaseType.POSTGRESQL, port = 6000).resolvedPort)
    }

    @Test
    fun databaseTypeFromTheEnvironmentPicksItsDefaultPort() {
        val config = DatabaseConfig().withEnvironment(DatabaseEnvironmentVariables.TYPE to "postgresql")

        assertEquals(5432, config.credentials.resolvedPort)
    }

    @Test
    fun portFromTheEnvironmentWins() {
        val config = DatabaseConfig(credentials = ConnectionDetails(port = 3307))
            .withEnvironment(DatabaseEnvironmentVariables.PORT to "6000")

        assertEquals(6000, config.credentials.resolvedPort)
    }

    @Test
    fun configuredPortIsKeptWithoutEnvironment() {
        val config = DatabaseConfig(credentials = ConnectionDetails(port = 3307)).withEnvironment()

        assertEquals(3307, config.credentials.resolvedPort)
    }

    @Test
    fun rejectsPortOutOfRange() {
        listOf("0", "65536").forEach {
            assertThrows<IllegalArgumentException>("port $it must be rejected") {
                DatabaseConfig().withEnvironment(DatabaseEnvironmentVariables.PORT to it)
            }
        }
    }
}
