package dev.slne.surf.database.reliability

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.reliability.operation.AtomicOperationConflictException
import dev.slne.surf.database.reliability.operation.AtomicOperationInProgressException
import dev.slne.surf.database.reliability.operation.AtomicOperations
import dev.slne.surf.database.reliability.operation.AtomicOperationsConfig
import dev.slne.surf.database.reliability.operation.OperationKey
import dev.slne.surf.database.reliability.operation.StoredReply
import dev.slne.surf.database.testing.MutableClock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Testcontainers(disabledWithoutDocker = true)
class AtomicOperationsIT {

    private val key = OperationKey("billing", "invoices.Create", "message-1")
    private val fingerprint = byteArrayOf(1, 2, 3)

    private fun reply(text: String) = StoredReply("text/plain", text.encodeToByteArray())

    private fun ReliabilityVendor.operationRows(api: DatabaseApi): Long =
        queryLong(api, "SELECT COUNT(*) FROM surf_atomic_operations")

    @ParameterizedTest
    @EnumSource(ReliabilityVendor::class)
    fun concurrentExecutionsOfOneKeyRunTheBlockOnceAndShareTheReply(vendor: ReliabilityVendor) =
        vendor.withTwoProcesses { first, second ->
            val runs = AtomicInteger()
            val claimed = CompletableDeferred<Unit>()
            val finish = CompletableDeferred<Unit>()

            coroutineScope {
                val original = async {
                    AtomicOperations(first).execute(key, fingerprint) {
                        runs.incrementAndGet()
                        claimed.complete(Unit)
                        finish.await()
                        reply("first")
                    }
                }
                claimed.await()
                val duplicate = async {
                    AtomicOperations(second).execute(key, fingerprint) {
                        runs.incrementAndGet()
                        reply("second")
                    }
                }
                vendor.awaitLockWaiters(first, 1)
                finish.complete(Unit)

                assertEquals(reply("first"), original.await())
                assertEquals(reply("first"), duplicate.await())
            }
            assertEquals(1, runs.get())
            assertEquals(1, vendor.operationRows(first))
        }

    @ParameterizedTest
    @EnumSource(ReliabilityVendor::class)
    fun crashBeforeCommitLetsAWaitingExecutionRunTheBlock(vendor: ReliabilityVendor) =
        vendor.withTwoProcesses { first, second ->
            val runs = AtomicInteger()
            val claimed = CompletableDeferred<Long>()
            val finish = CompletableDeferred<Unit>()
            val survivor = AtomicOperations(second)

            coroutineScope {
                val crashing = async {
                    runCatching {
                        AtomicOperations(first).execute(key, fingerprint) {
                            runs.incrementAndGet()
                            claimed.complete(with(vendor) { transaction.connectionId() })
                            finish.await()
                            reply("crashed")
                        }
                    }
                }
                val connectionId = claimed.await()
                // Two waiters: when the holder dies, both get the lock; MariaDB resolves this with a deadlock.
                val waiters = List(2) { index ->
                    async {
                        survivor.execute(key, fingerprint) {
                            runs.incrementAndGet()
                            reply("survivor-$index")
                        }
                    }
                }
                vendor.awaitLockWaiters(first, 2)

                vendor.kill(first, connectionId)
                val replies = waiters.awaitAll()
                finish.complete(Unit)

                assertTrue(crashing.await().isFailure, "the crashed execution must not succeed")
                assertEquals(1, replies.toSet().size, "both waiters must return the same reply: $replies")
                assertTrue(replies.first().bytes.decodeToString().startsWith("survivor-"))
            }
            assertEquals(2, runs.get(), "the crashed run and exactly one survivor run")
            assertEquals(1, vendor.operationRows(second))
        }

    @ParameterizedTest
    @EnumSource(ReliabilityVendor::class)
    fun crashAfterCommitReturnsTheStoredReplyWithoutRunningTheBlock(vendor: ReliabilityVendor) =
        vendor.withTwoProcesses { first, second ->
            AtomicOperations(first).execute(key, fingerprint) { reply("original") }

            val replayed = AtomicOperations(second).execute(key, fingerprint) { error("must not run again") }

            assertEquals(reply("original"), replayed)
        }

    @ParameterizedTest
    @EnumSource(ReliabilityVendor::class)
    fun sameKeyWithAnotherFingerprintConflicts(vendor: ReliabilityVendor) =
        vendor.withTwoProcesses { first, second ->
            AtomicOperations(first).execute(key, fingerprint) { reply("original") }

            val conflict = assertFailsWith<AtomicOperationConflictException> {
                AtomicOperations(second).execute(key, byteArrayOf(9)) { error("must not run") }
            }

            assertEquals(key, conflict.key)
        }

    @ParameterizedTest
    @EnumSource(ReliabilityVendor::class)
    fun keysDifferingOnlyInCaseAreSeparateOperations(vendor: ReliabilityVendor) =
        vendor.withTwoProcesses { first, _ ->
            val operations = AtomicOperations(first)
            operations.execute(key, fingerprint) { reply("lower") }

            val upper = operations.execute(key.copy(idempotencyKey = "MESSAGE-1"), fingerprint) { reply("upper") }

            assertEquals(reply("upper"), upper)
            assertEquals(2, vendor.operationRows(first))
        }

    @ParameterizedTest
    @EnumSource(ReliabilityVendor::class)
    fun keysDifferingOnlyInTrailingSpacesAreSeparateOperations(vendor: ReliabilityVendor) =
        vendor.withTwoProcesses { first, _ ->
            val operations = AtomicOperations(first)
            operations.execute(key, fingerprint) { reply("plain") }

            val padded = operations.execute(key.copy(idempotencyKey = "message-1 "), fingerprint) { reply("padded") }

            assertEquals(reply("padded"), padded)
            assertEquals(2, vendor.operationRows(first))
        }

    @ParameterizedTest
    @EnumSource(ReliabilityVendor::class)
    fun aWaitLongerThanTheLockTimeoutFailsWithoutRunningTheBlock(vendor: ReliabilityVendor) =
        vendor.withTwoProcesses { first, second ->
            val claimed = CompletableDeferred<Unit>()
            val finish = CompletableDeferred<Unit>()

            coroutineScope {
                val holder = async {
                    AtomicOperations(first).execute(key, fingerprint) {
                        claimed.complete(Unit)
                        finish.await()
                        reply("holder")
                    }
                }
                claimed.await()

                val impatient = AtomicOperations(second, AtomicOperationsConfig(lockTimeout = 1.seconds))
                val failure = assertFailsWith<AtomicOperationInProgressException> {
                    impatient.execute(key, fingerprint) { error("must not run without the claim") }
                }
                finish.complete(Unit)

                assertEquals(key, failure.key)
                assertEquals(reply("holder"), holder.await())
            }
        }

    @ParameterizedTest
    @EnumSource(ReliabilityVendor::class)
    fun theBlockKeepsTheSessionLockTimeoutAfterTheBoundedClaim(vendor: ReliabilityVendor) =
        vendor.withTwoProcesses(sessionLockTimeout = true) { first, _ ->
            val operations = AtomicOperations(first, AtomicOperationsConfig(lockTimeout = 1.seconds))

            val inBlock = operations.execute(key, fingerprint) {
                with(vendor) { reply(transaction.lockTimeout()) }
            }

            assertEquals(reply(vendor.sessionLockTimeout), inBlock)
        }

    @ParameterizedTest
    @EnumSource(ReliabilityVendor::class)
    fun anExpiredReplyIsExecutedAgainAndCleanedUp(vendor: ReliabilityVendor) =
        vendor.withTwoProcesses { first, second ->
            val clock = MutableClock()
            val config = AtomicOperationsConfig(retention = 1.hours, clock = clock)
            val operations = AtomicOperations(first, config)
            operations.execute(key, fingerprint) { reply("old") }
            operations.execute(key.copy(idempotencyKey = "message-2"), fingerprint) { reply("kept") }
            clock.advance(30.minutes)
            operations.execute(key.copy(idempotencyKey = "message-3"), fingerprint) { reply("young") }

            clock.advance(31.minutes)
            val renewed = AtomicOperations(second, config).execute(key, fingerprint) { reply("new") }
            assertEquals(reply("new"), renewed)

            val deleted = operations.deleteExpired(limit = 10)
            assertEquals(1, deleted, "only message-2 is still expired")
            assertEquals(2, vendor.operationRows(first))
            assertEquals(0, operations.deleteExpired())
        }

    @ParameterizedTest
    @EnumSource(ReliabilityVendor::class)
    fun deletingAnExpiredReplyDoesNotConsumeTheOnlyAttempt(vendor: ReliabilityVendor) =
        vendor.withTwoProcesses { first, _ ->
            val clock = MutableClock()
            val config = AtomicOperationsConfig(retention = 1.hours, maxAttempts = 1, clock = clock)
            val operations = AtomicOperations(first, config)
            operations.execute(key, fingerprint) { reply("old") }
            clock.advance(2.hours)

            val renewed = operations.execute(key, fingerprint) { reply("new") }

            assertEquals(reply("new"), renewed)
            assertEquals(1, vendor.operationRows(first))
        }

    @ParameterizedTest
    @EnumSource(ReliabilityVendor::class)
    @Timeout(LOCKED_DELETE_DEADLINE_SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    fun deletingALockedExpiredReplyWaitsAtMostTheLockTimeout(vendor: ReliabilityVendor) =
        vendor.withTwoProcesses { first, second ->
            val clock = MutableClock()
            val config = AtomicOperationsConfig(retention = 1.hours, lockTimeout = 1.seconds, clock = clock)
            AtomicOperations(first, config).execute(key, fingerprint) { reply("old") }
            clock.advance(2.hours)
            val operations = AtomicOperations(second, config)

            val failure = vendor.holdShareLock(first, key).use {
                assertFailsWith<AtomicOperationInProgressException> {
                    operations.execute(key, fingerprint) { error("must not run while the expired row is locked") }
                }
            }

            assertEquals(key, failure.key)
            assertEquals(reply("new"), operations.execute(key, fingerprint) { reply("new") })
        }

    @ParameterizedTest
    @EnumSource(ReliabilityVendor::class)
    fun deleteExpiredHonorsTheBatchLimit(vendor: ReliabilityVendor) = vendor.withTwoProcesses { first, _ ->
        val clock = MutableClock()
        val operations = AtomicOperations(first, AtomicOperationsConfig(retention = 1.days, clock = clock))
        repeat(5) { operations.execute(key.copy(idempotencyKey = "m$it"), fingerprint) { reply("r$it") } }
        clock.advance(2.days)

        assertEquals(3, operations.deleteExpired(limit = 3))
        assertEquals(2, operations.deleteExpired(limit = 3))
        assertEquals(0, vendor.operationRows(first))
    }

    @ParameterizedTest
    @EnumSource(ReliabilityVendor::class)
    fun aFailingBlockStoresNothing(vendor: ReliabilityVendor) = vendor.withTwoProcesses { first, _ ->
        val operations = AtomicOperations(first)

        assertFailsWith<IllegalArgumentException> {
            operations.execute(key, fingerprint) {
                throw IllegalArgumentException("handler failed")
            }
        }

        assertEquals(0, vendor.operationRows(first))
        assertEquals(reply("retried"), operations.execute(key, fingerprint) { reply("retried") })
    }

    @ParameterizedTest
    @EnumSource(ReliabilityVendor::class)
    fun executeInsideAnOpenTransactionFails(vendor: ReliabilityVendor) = vendor.withTwoProcesses { first, _ ->
        val operations = AtomicOperations(first)

        assertFailsWith<IllegalStateException> {
            suspendTransaction(db = first.database) {
                operations.execute(key, fingerprint) { reply("never") }
            }
        }
        assertEquals(0, vendor.operationRows(first))
    }

    @ParameterizedTest
    @EnumSource(ReliabilityVendor::class)
    fun largeRepliesRoundTrip(vendor: ReliabilityVendor) = vendor.withTwoProcesses { first, second ->
        val bytes = ByteArray(2 * 1024 * 1024) { (it % 251).toByte() }
        AtomicOperations(first).execute(key, fingerprint) { StoredReply("application/octet-stream", bytes) }

        val replayed = AtomicOperations(second).execute(key, fingerprint) { error("must not run again") }

        assertContentEquals(bytes, replayed.bytes)
    }

    private companion object {
        /**
         * Upper bound for a delete that waits on a lock with a lock timeout of one second, far below the server
         * defaults of 50 seconds (MariaDB) and no limit (PostgreSQL). Enforced on a separate thread, since a delete
         * waiting on the server does not react to cancellation.
         */
        const val LOCKED_DELETE_DEADLINE_SECONDS = 20L
    }
}
