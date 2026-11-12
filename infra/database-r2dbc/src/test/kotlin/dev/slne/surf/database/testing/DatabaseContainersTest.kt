package dev.slne.surf.database.testing

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Testcontainers(disabledWithoutDocker = true)
class DatabaseContainersTest {
    @Test
    fun mariaDbFreshApiWorksOverR2dbcAndJdbc() = runTest {
        val api = DatabaseContainers.mariadb().toDatabaseApi(fresh = true)
        try {
            val name = suspendTransaction(db = api.database) {
                exec("SELECT DATABASE()") { it.get(0, String::class.java) }!!.first()
            }
            assertNotNull(api.migrationDataSource).connection.use { assertEquals(name, it.catalog) }
        } finally {
            api.shutdown()
        }
    }

    @Test
    fun postgresFreshApiWorksOverR2dbcAndJdbc() = runTest {
        val api = DatabaseContainers.postgres().toDatabaseApi(fresh = true)
        try {
            val name = suspendTransaction(db = api.database) {
                exec("SELECT current_schema()") { it.get(0, String::class.java) }!!.first()
            }
            assertNotNull(api.migrationDataSource).connection.use { assertEquals(name, it.schema) }
        } finally {
            api.shutdown()
        }
    }
}
