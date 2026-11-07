package dev.slne.surf.database.reliability.operation

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.query.allOf
import dev.slne.surf.database.reliability.internal.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.r2dbc.update
import java.time.Instant
import kotlin.time.toJavaDuration

/**
 * Executes operations at most once per [OperationKey] and stores their replies on one database.
 *
 * [execute] claims the key, runs the block, stores the block's reply and commits all of it in one READ COMMITTED
 * transaction. A repeated execution of a committed key returns the stored reply without running the block. There is
 * no separately committed "processing" state: a crash before the commit leaves nothing behind, and the next execution
 * runs the block again.
 *
 * Several processes may share the tables, each with its own instance. Requires the schema of
 * [migrateReliabilitySchema][dev.slne.surf.database.reliability.migrateReliabilitySchema].
 *
 * @throws IllegalStateException if [databaseApi] is neither MariaDB nor PostgreSQL
 */
class AtomicOperations(
    private val databaseApi: DatabaseApi,
    private val config: AtomicOperationsConfig = AtomicOperationsConfig(),
) {
    private val claim = OperationClaim(databaseApi.databaseType, config.lockTimeout)

    /**
     * Executes [block] once for [key] and returns its reply, or returns the reply stored by an earlier execution.
     *
     * Concurrent executions of the same key wait for each other: the first one runs [block], the others return its
     * reply after it commits, or one of them runs [block] if it rolls back. A wait longer than
     * [AtomicOperationsConfig.lockTimeout] fails without running [block].
     *
     * [block] runs inside the operation's transaction: its statements on [DatabaseApi.database] and the stored reply
     * commit or roll back together. Statements on other databases and other side effects are not covered. If [block] throws, the transaction rolls back, nothing is
     * stored and the exception propagates. After a deadlock or serialization failure the whole transaction is
     * retried, up to [AtomicOperationsConfig.maxAttempts] times, so [block] can run more than once per call; only
     * the last run commits.
     *
     * A stored reply expires after [AtomicOperationsConfig.retention]; an execution after that deletes it and runs
     * [block] again. Deleting the expired reply does not count as an attempt.
     *
     * @param requestFingerprint Hash of the request, 1 to 64 bytes. A repeated execution with another fingerprint is
     * rejected, so a reused key cannot return the reply of a different request.
     * @throws IllegalStateException if called inside an open transaction, or the attempts are exhausted by claims
     * that vanished concurrently
     * @throws IllegalArgumentException if [requestFingerprint] is empty or longer than 64 bytes
     * @throws AtomicOperationConflictException if [key] was executed with another fingerprint
     * @throws AtomicOperationInProgressException if a concurrent execution held [key] longer than the lock timeout,
     * also while an expired reply is deleted
     */
    suspend fun execute(
        key: OperationKey,
        requestFingerprint: ByteArray,
        block: suspend AtomicOperationScope.() -> StoredReply,
    ): StoredReply {
        check(TransactionManager.currentOrNull() == null) {
            "Operation $key must not run inside an open transaction; it owns its transaction"
        }

        require(requestFingerprint.size in 1..MAX_FINGERPRINT_BYTES) {
            "requestFingerprint must have 1 to $MAX_FINGERPRINT_BYTES bytes, had ${requestFingerprint.size}"
        }

        var lastFailure: Throwable? = null
        repeat(config.maxAttempts) {
            when (val attempt = attempt(key, requestFingerprint, block)) {
                is Attempt.Completed -> return attempt.reply
                is Attempt.Retry -> lastFailure = attempt.cause
            }
        }

        lastFailure?.let { if (SqlFailure.of(it) == SqlFailure.TRANSIENT_CONFLICT) throw it }
        throw IllegalStateException(
            "Operation $key did not complete in ${config.maxAttempts} attempts",
            lastFailure
        )
    }

    /**
     * Deletes up to [limit] expired operations of all owners and returns how many were deleted. Rows locked by a
     * concurrent cleanup are skipped.
     *
     * @throws IllegalArgumentException if [limit] is not positive
     */
    suspend fun deleteExpired(limit: Int = DEFAULT_CLEANUP_BATCH_SIZE): Int {
        require(limit > 0) { "limit must be positive, was $limit" }

        return databaseApi.readCommittedTransaction {
            val expired = AtomicOperationTable.select(
                AtomicOperationTable.owner,
                AtomicOperationTable.operation,
                AtomicOperationTable.idempotencyKey
            )
                .where { AtomicOperationTable.expiresAt lessEq config.clock.instant() }
                .orderBy(AtomicOperationTable.expiresAt)
                .limit(limit)
                .forUpdate(databaseApi.databaseType.forUpdateSkipLocked())
                .map { row ->
                    Triple(
                        row[AtomicOperationTable.owner],
                        row[AtomicOperationTable.operation],
                        row[AtomicOperationTable.idempotencyKey],
                    )
                }
                .toList()

            if (expired.isEmpty()) {
                0
            } else {
                AtomicOperationTable.deleteWhere {
                    Triple(
                        AtomicOperationTable.owner,
                        AtomicOperationTable.operation,
                        AtomicOperationTable.idempotencyKey,
                    ) inList expired
                }
            }
        }
    }

    /**
     * One of the [AtomicOperationsConfig.maxAttempts] attempts of [execute]. Deleting an expired reply and claiming
     * the key again belong to the same attempt, so an expired reply does not consume an attempt.
     */
    private suspend fun attempt(
        key: OperationKey,
        requestFingerprint: ByteArray,
        block: suspend AtomicOperationScope.() -> StoredReply,
    ): Attempt = try {
        claimOrReplay(key, requestFingerprint, block)
            ?: claimOrReplay(key, requestFingerprint, block)
            ?: Attempt.Retry(null)
    } catch (e: Exception) {
        if (SqlFailure.of(e) != SqlFailure.TRANSIENT_CONFLICT) throw e
        Attempt.Retry(e)
    }

    /**
     * Claims [key] and runs [block], or replays the reply of the execution that holds the key.
     *
     * @return `null` if the stored reply had expired and was deleted, so that the key can be claimed again
     */
    private suspend fun claimOrReplay(
        key: OperationKey,
        requestFingerprint: ByteArray,
        block: suspend AtomicOperationScope.() -> StoredReply,
    ): Attempt? = try {
        Attempt.Completed(executeClaimed(key, requestFingerprint, block))
    } catch (_: ClaimTakenException) {
        replayStored(key, requestFingerprint)
    }

    private suspend fun executeClaimed(
        key: OperationKey,
        requestFingerprint: ByteArray,
        block: suspend AtomicOperationScope.() -> StoredReply,
    ): StoredReply = databaseApi.readCommittedTransaction {
        val now = config.clock.instant()
        claim.insert(
            transaction = this,
            key = key,
            fingerprint = requestFingerprint,
            now = now,
            expiresAt = now.plus(config.retention.toJavaDuration()),
        )

        val reply = ActiveOperationScope(key, this).block()
        storeReply(key, reply)
        reply
    }

    private suspend fun storeReply(key: OperationKey, reply: StoredReply) {
        val updated = AtomicOperationTable.update(where = { key.matches() }) { stm ->
            stm[responseFormat] = reply.format
            stm[responseBytes] = reply.bytes
            stm[completedAt] = config.clock.instant()
        }
        check(updated == 1) { "Claim of operation $key disappeared before its reply was stored" }
    }

    /**
     * @return `null` if the stored reply had expired and was deleted
     */
    private suspend fun replayStored(key: OperationKey, requestFingerprint: ByteArray): Attempt? {
        val now = config.clock.instant()
        val stored = databaseApi.readCommittedTransaction {
            AtomicOperationTable.selectAll()
                .where(key.matches())
                .map(::StoredOperation)
                .singleOrNull()
        } ?: return Attempt.Retry(null)

        return when {
            !stored.expiresAt.isAfter(now) -> {
                databaseApi.readCommittedTransaction {
                    claim.deleteIfExpired(transaction = this, key = key, now = now)
                }
                null
            }

            !stored.requestFingerprint.contentEquals(requestFingerprint) -> {
                throw AtomicOperationConflictException(key)
            }

            else -> Attempt.Completed(
                checkNotNull(stored.reply) {
                    "Operation $key was committed without a reply"
                }
            )
        }
    }

    private fun OperationKey.matches(): Op<Boolean> = allOf(
        AtomicOperationTable.owner eq this.owner,
        AtomicOperationTable.operation eq this.operation,
        AtomicOperationTable.idempotencyKey eq this.idempotencyKey,
    )

    private sealed interface Attempt {
        class Completed(val reply: StoredReply) : Attempt

        /**
         * @property cause Transient database failure, or `null` if the stored row vanished or expired concurrently.
         */
        class Retry(val cause: Throwable?) : Attempt
    }

    private class StoredOperation(row: ResultRow) {
        val requestFingerprint = row[AtomicOperationTable.requestFingerprint]
        val expiresAt: Instant = row[AtomicOperationTable.expiresAt]
        val reply = row[AtomicOperationTable.responseFormat]?.let { format ->
            StoredReply(format, row[AtomicOperationTable.responseBytes] ?: ByteArray(0))
        }
    }

    companion object {
        /**
         * Maximum size of a request fingerprint, enough for SHA-512.
         */
        const val MAX_FINGERPRINT_BYTES = 64

        /**
         * Default batch size of [deleteExpired].
         */
        const val DEFAULT_CLEANUP_BATCH_SIZE = 1000
    }
}
