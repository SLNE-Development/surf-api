package dev.slne.surf.database.transaction

import dev.slne.surf.database.testing.DatabaseContainers
import dev.slne.surf.database.testing.TestDatabase
import dev.slne.surf.database.testing.connectionDetails
import dev.slne.surf.database.testing.toTestDatabase
import dev.slne.surf.database.utils.isDuplicateKeyViolation
import dev.slne.surf.database.utils.isTransientConflict
import io.r2dbc.spi.IsolationLevel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.testcontainers.containers.JdbcDatabaseContainer
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Forces real conflicts between concurrent transactions and checks which of them
 * [retryingTransaction] repeats.
 */
class RetryingTransactionIT {

    enum class Vendor(val container: () -> JdbcDatabaseContainer<*>) {
        MARIADB(DatabaseContainers::mariadb),
        POSTGRESQL(DatabaseContainers::postgres),
    }

    @ParameterizedTest
    @EnumSource(Vendor::class)
    fun `the victim of a deadlock runs again and commits`(vendor: Vendor) = withAccounts(vendor) { testDb ->
        val attempts = AtomicInteger()

        crossTransfers(testDb, attempts, maxAttempts = 3).awaitAll()

        assertEquals(3, attempts.get(), "both transfers ran once and the deadlock victim once more")
        assertEquals(listOf(100, 100), balances(testDb))
    }

    @ParameterizedTest
    @EnumSource(Vendor::class)
    fun `the last attempt rethrows the conflict`(vendor: Vendor) = withAccounts(vendor) { testDb ->
        val results = supervisorScope {
            crossTransfers(
                testDb,
                AtomicInteger(),
                maxAttempts = 1,
            ).map { runCatching { it.await() } }
        }

        val failures = results.mapNotNull { it.exceptionOrNull() }
        assertEquals(1, failures.size, "one transfer is the deadlock victim")
        assertTrue(failures.single().isTransientConflict(), "the victim fails with the deadlock")
    }

    @ParameterizedTest
    @EnumSource(Vendor::class)
    fun `a write conflict under repeatable read runs again on a new snapshot`(vendor: Vendor) = withAccounts(
        vendor,
    ) { testDb ->
        val attempts = AtomicInteger()
        val snapshotTaken = CompletableDeferred<Unit>()
        val concurrentUpdateCommitted = CompletableDeferred<Unit>()

        val reader = async(Dispatchers.Default) {
            testDb.database.retryingTransaction(IsolationLevel.REPEATABLE_READ) {
                val attempt = attempts.incrementAndGet()
                balanceOf(1)

                if (attempt == 1) {
                    snapshotTaken.complete(Unit)
                    concurrentUpdateCommitted.await()
                }

                exec("UPDATE accounts SET balance = balance + 1 WHERE id = 1")
            }
        }
        snapshotTaken.await()
        suspendTransaction(testDb.database) {
            exec("UPDATE accounts SET balance = balance + 10 WHERE id = 1")
        }
        concurrentUpdateCommitted.complete(Unit)
        reader.await()

        assertEquals(2, attempts.get())
        assertEquals(listOf(111, 100), balances(testDb))
    }

    @ParameterizedTest
    @EnumSource(Vendor::class)
    fun `other failures are not retried`(vendor: Vendor) = withAccounts(vendor) { testDb ->
        val attempts = AtomicInteger()

        val failure = assertFailsWith<Exception> {
            testDb.database.retryingTransaction {
                attempts.incrementAndGet()
                exec("INSERT INTO accounts (id, balance) VALUES (1, 0)")
            }
        }

        assertTrue(failure.isDuplicateKeyViolation())
        assertEquals(1, attempts.get())
    }

    @Test
    fun `an open transaction is rejected`() = withAccounts(Vendor.MARIADB) { testDb ->
        suspendTransaction(testDb.database) {
            assertFailsWith<IllegalStateException> { testDb.database.retryingTransaction { } }
        }
    }

    /**
     * Starts two transfers that lock the two accounts in opposite order; both wait until each holds
     * its first lock on their first attempt, so exactly one of them becomes the deadlock victim.
     */
    private fun CoroutineScope.crossTransfers(
        testDb: TestDatabase,
        attempts: AtomicInteger,
        maxAttempts: Int,
    ) = listOf(1 to 2, 2 to 1).let { orders ->
        val firstLocks = orders.map { CompletableDeferred<Unit>() }
        orders.mapIndexed { index, (from, to) ->
            async(Dispatchers.Default) {
                testDb.database.retryingTransaction(maxAttempts = maxAttempts) {
                    val attempt = attempts.incrementAndGet()
                    exec("UPDATE accounts SET balance = balance - 1 WHERE id = $from")

                    if (firstLocks[index].complete(Unit)) {
                        for (firstLock in firstLocks) {
                            firstLock.await()
                        }
                    }

                    exec("UPDATE accounts SET balance = balance + 1 WHERE id = $to")
                    attempt
                }
            }
        }
    }

    private fun withAccounts(
        vendor: Vendor,
        block: suspend CoroutineScope.(TestDatabase) -> Unit,
    ) = runBlocking {
        val testDb = vendor.container().connectionDetails(fresh = true).toTestDatabase()

        try {
            suspendTransaction(testDb.database) {
                exec("CREATE TABLE accounts (id INT PRIMARY KEY, balance INT NOT NULL)")
                exec("INSERT INTO accounts (id, balance) VALUES (1, 100), (2, 100)")
            }
            block(testDb)
        } finally {
            testDb.close()
        }
    }

    private suspend fun balances(testDb: TestDatabase): List<Int> {
        return suspendTransaction(testDb.database) {
            val balances = exec("SELECT balance FROM accounts ORDER BY id") { row ->
                (row.get(0) as Number).toInt()
            }
            assertNotNull(balances)
                .toList()
                .requireNoNulls()
        }
    }

    private suspend fun R2dbcTransaction.balanceOf(id: Int): Int {
        return assertNotNull(
            exec("SELECT balance FROM accounts WHERE id = $id") { (it.get(0) as Number).toInt() },
        )
            .toList()
            .requireNoNulls()
            .single()
    }
}
