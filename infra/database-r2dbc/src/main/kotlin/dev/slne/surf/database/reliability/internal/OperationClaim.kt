package dev.slne.surf.database.reliability.internal

import dev.slne.surf.database.config.DatabaseType
import dev.slne.surf.database.reliability.operation.AtomicOperationInProgressException
import dev.slne.surf.database.reliability.operation.OperationKey
import io.r2dbc.spi.R2dbcException
import kotlinx.coroutines.flow.single
import org.intellij.lang.annotations.Language
import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.VarCharColumnType
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import java.time.Instant
import kotlin.time.Duration

/**
 * Statements on the row of an operation key that wait at most [lockTimeout] for a concurrent execution of the same
 * key.
 *
 * The wait is bounded per statement: `SET STATEMENT … FOR` on MariaDB; on PostgreSQL a transaction-local
 * `lock_timeout` around the statement, after which the previous value is restored. Statements of the operation itself
 * keep the lock timeout that was in effect before the claim. On both vendors, a wait longer
 * than [lockTimeout] fails with [AtomicOperationInProgressException].
 *
 * Every exception of a claim statement requires the caller to roll back the whole transaction. PostgreSQL aborts the
 * transaction on any error, so the previous lock timeout is only restored after a successful statement; MariaDB rolls
 * back only the failed statement, and committing the rest would persist work without a claim.
 */
internal class OperationClaim(
    private val databaseType: DatabaseType,
    private val lockTimeout: Duration,
) {

    /**
     * Inserts the claim row of [key].
     *
     * @throws ClaimTakenException if a committed row with the same key exists; the transaction must be abandoned
     * @throws AtomicOperationInProgressException if the key stayed locked for [lockTimeout]
     */
    suspend fun insert(
        transaction: R2dbcTransaction,
        key: OperationKey,
        fingerprint: ByteArray,
        now: Instant,
        expiresAt: Instant,
    ) {
        val arguments = key.arguments() + listOf(
            ByteArrayColumnType() to fingerprint,
            UtcInstantColumnType() to now,
            UtcInstantColumnType() to expiresAt,
        )

        try {
            execBounded(
                transaction = transaction,
                key = key,
                sql = INSERT,
                statementType = StatementType.INSERT,
                arguments = arguments,
            )
        } catch (e: R2dbcException) {
            if (SqlFailure.of(e) == SqlFailure.DUPLICATE_KEY) throw ClaimTakenException()
            throw e
        }
    }

    /**
     * Deletes the row of [key] if it expired at [now].
     *
     * @throws AtomicOperationInProgressException if the key stayed locked for [lockTimeout]
     */
    suspend fun deleteIfExpired(transaction: R2dbcTransaction, key: OperationKey, now: Instant) {
        execBounded(
            transaction = transaction,
            key = key,
            sql = DELETE_EXPIRED,
            statementType = StatementType.DELETE,
            arguments = key.arguments() + (UtcInstantColumnType() to now),
        )
    }

    private suspend fun execBounded(
        transaction: R2dbcTransaction,
        key: OperationKey,
        sql: String,
        statementType: StatementType,
        arguments: List<Pair<IColumnType<*>, Any?>>,
    ) {
        try {
            when (databaseType) {
                DatabaseType.MARIADB -> {
                    transaction.exec(
                        stmt = "SET STATEMENT innodb_lock_wait_timeout = ${mariaDbSeconds()} FOR $sql",
                        args = arguments,
                        explicitStatementType = statementType,
                    )
                }

                DatabaseType.POSTGRESQL -> {
                    val previousLockTimeout = transaction.currentLockTimeout()
                    transaction.setLocalLockTimeout("${lockTimeout.inWholeMilliseconds}ms")
                    transaction.exec(stmt = sql, args = arguments, explicitStatementType = statementType)
                    transaction.setLocalLockTimeout(previousLockTimeout)
                }
            }
        } catch (e: R2dbcException) {
            if (SqlFailure.of(e) == SqlFailure.LOCK_TIMEOUT) {
                throw AtomicOperationInProgressException(key, e)
            }
            throw e
        }
    }

    private suspend fun R2dbcTransaction.currentLockTimeout(): String {
        val timeout = exec(CURRENT_LOCK_TIMEOUT) { row ->
            row.get(0, String::class.java)
        }

        return checkNotNull(timeout).single() ?: error("PostgreSQL returned no lock_timeout")
    }

    private suspend fun R2dbcTransaction.setLocalLockTimeout(value: String) {
        exec(SET_LOCAL_LOCK_TIMEOUT, listOf(TextColumnType() to value))
    }

    private fun OperationKey.arguments(): List<Pair<IColumnType<*>, Any?>> = listOf(
        VarCharColumnType() to owner,
        VarCharColumnType() to operation,
        VarCharColumnType() to idempotencyKey,
    )

    /**
     * [lockTimeout] rounded up to the whole seconds of `innodb_lock_wait_timeout`.
     *
     * [AtomicOperationsConfig][dev.slne.surf.database.reliability.operation.AtomicOperationsConfig] bounds
     * [lockTimeout] to 1 ms .. [Int.MAX_VALUE] ms, so the result is 1 to 2,147,484 seconds: never `0`, which would
     * not wait at all, and within MariaDB's limit of 100,000,000 seconds.
     */
    private fun mariaDbSeconds(): Long = Math.ceilDiv(lockTimeout.inWholeMilliseconds, MILLIS_PER_SECOND)

    private companion object {
        const val MILLIS_PER_SECOND = 1000L
        const val INSERT = "INSERT INTO surf_atomic_operations " +
                "(owner, operation, idempotency_key, request_fingerprint, created_at, expires_at) " +
                "VALUES (?, ?, ?, ?, ?, ?)"
        const val CURRENT_LOCK_TIMEOUT = "SELECT current_setting('lock_timeout')"
        const val SET_LOCAL_LOCK_TIMEOUT = "SELECT set_config('lock_timeout', ?, true)"
        const val DELETE_EXPIRED = "DELETE FROM surf_atomic_operations " +
                "WHERE owner = ? AND operation = ? AND idempotency_key = ? AND expires_at <= ?"
    }
}
