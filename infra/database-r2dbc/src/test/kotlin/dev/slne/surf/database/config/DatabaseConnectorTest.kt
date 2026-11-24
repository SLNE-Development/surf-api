package dev.slne.surf.database.config

import dev.slne.surf.database.testing.withOfflineDatabase
import io.r2dbc.spi.IsolationLevel
import org.jetbrains.exposed.v1.core.vendors.H2Dialect
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.r2dbc.transactions.transactionManager
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Checks the defaults that `DatabaseConnector` applies to a database and how its dialect maps to a
 * [DatabaseType]. Nothing here connects to a database.
 */
class DatabaseConnectorTest {

    @Test
    fun transactionsDefaultToReadCommitted() = withOfflineDatabase { database ->
        assertEquals(
            IsolationLevel.READ_COMMITTED,
            database.transactionManager.defaultIsolationLevel,
        )
    }

    @Test
    fun customizerCanOverrideTheIsolationLevel() {
        val level = withOfflineDatabase(
            customize = { defaultR2dbcIsolationLevel = IsolationLevel.SERIALIZABLE },
        ) { database ->
            database.transactionManager.defaultIsolationLevel
        }

        assertEquals(IsolationLevel.SERIALIZABLE, level)
    }

    @Test
    fun customizerRunsWhenTheDatabaseIsConnected() {
        var customized = false

        withOfflineDatabase(customize = { customized = true }) {}

        assertTrue(customized)
    }

    @Test
    fun databaseTypeIsDerivedFromTheDialect() {
        assertEquals(DatabaseType.MARIADB, withOfflineDatabase { it.databaseType })
        assertEquals(
            DatabaseType.POSTGRESQL,
            withOfflineDatabase(dialect = PostgreSQLDialect()) { it.databaseType },
        )
    }

    @Test
    fun databaseTypeOfADialectWithoutDatabaseTypeFails() {
        val failure = withOfflineDatabase(dialect = H2Dialect()) { database ->
            assertFailsWith<IllegalStateException> { database.databaseType }
        }

        assertContains(failure.message.orEmpty(), "H2")
    }

    @Test
    fun vendorIsTheLowercaseTypeName() {
        assertEquals(listOf("mariadb", "postgresql"), DatabaseType.entries.map { it.vendor })
    }
}
