package dev.slne.surf.database.testing

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals

@Testcontainers(disabledWithoutDocker = true)
class DatabaseContainersTest {
    @Test
    fun mariaDbFreshDatabaseWorksOverR2dbcAndJdbc() = runTest {
        val testDb = DatabaseContainers.mariadb().toTestDatabase(fresh = true)

        try {
            val name = suspendTransaction(db = testDb.database) {
                exec("SELECT DATABASE()") { it.get(0, String::class.java) }!!.first()
            }
            testDb.migrationDataSource.connection.use { assertEquals(name, it.catalog) }
        } finally {
            testDb.close()
        }
    }

    @Test
    fun postgresFreshDatabaseWorksOverR2dbcAndJdbc() = runTest {
        val testDb = DatabaseContainers.postgres().toTestDatabase(fresh = true)

        try {
            val name = suspendTransaction(db = testDb.database) {
                exec("SELECT current_schema()") { it.get(0, String::class.java) }!!.first()
            }
            testDb.migrationDataSource.connection.use { assertEquals(name, it.schema) }
        } finally {
            testDb.close()
        }
    }
}
