package dev.slne.surf.database

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DatabaseContainerSmokeTest : DatabaseTestBase() {

    @Test
    fun containerAcceptsConnections() = runBlocking {
        suspendTransaction() {
            exec("SELECT 1") { result ->
                assertEquals(result.get(1), 1)
            }
        }

        Unit
    }
}