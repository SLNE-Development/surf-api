package dev.slne.surf.database

import dev.slne.surf.database.reliability.ReliabilityVendor
import dev.slne.surf.database.testing.DatabaseContainers
import dev.slne.surf.database.testing.connectionDetails
import dev.slne.surf.database.testing.toTestDatabase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals

/**
 * Checks that transactions default to READ COMMITTED on a live database.
 */
@Testcontainers(disabledWithoutDocker = true)
class DefaultIsolationIT {

    @Test
    fun mariaDbTransactionsDoNotReadUncommittedRows() = runBlocking {
        val details = DatabaseContainers.mariadb().connectionDetails(fresh = true)
        val writer = details.toTestDatabase()
        val reader = details.toTestDatabase()

        try {
            ReliabilityVendor.MARIADB.execute(writer, "CREATE TABLE items (id INT PRIMARY KEY)")
            val inserted = CompletableDeferred<Unit>()
            val finish = CompletableDeferred<Unit>()

            val pending = async {
                suspendTransaction(db = writer.database) {
                    exec("INSERT INTO items (id) VALUES (1)")
                    inserted.complete(Unit)
                    finish.await()
                }
            }
            inserted.await()
            val visible = suspendTransaction(db = reader.database) {
                exec("SELECT COUNT(*) FROM items") { (it.get(0) as Number).toLong() }!!.single()
            }
            finish.complete(Unit)
            pending.await()

            assertEquals(0L, visible)
        } finally {
            writer.close()
            reader.close()
        }
    }

    @Test
    fun postgresTransactionsRunReadCommitted() = runBlocking {
        val testDb = DatabaseContainers.postgres().toTestDatabase(fresh = true)

        try {
            val level = suspendTransaction(db = testDb.database) {
                exec("SELECT current_setting('transaction_isolation')") {
                    it.get(
                        0,
                        String::class.java,
                    )
                }!!.single()
            }

            assertEquals("read committed", level)
        } finally {
            testDb.close()
        }
    }
}
