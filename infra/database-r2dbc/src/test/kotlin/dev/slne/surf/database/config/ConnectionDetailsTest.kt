package dev.slne.surf.database.config

import dev.slne.surf.api.config.type.number.IntOr
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ConnectionDetailsTest {

    @Test
    fun defaultPortDependsOnTheDatabaseType() {
        assertEquals(3306, ConnectionDetails(databaseType = DatabaseType.MARIADB).resolvedPort)
        assertEquals(5432, ConnectionDetails(databaseType = DatabaseType.POSTGRESQL).resolvedPort)
    }

    @Test
    fun configuredPortWins() {
        val details = ConnectionDetails(databaseType = DatabaseType.POSTGRESQL, port = IntOr.Default(6000))

        assertEquals(6000, details.resolvedPort)
    }
}
