package dev.slne.surf.database.transaction

import dev.slne.surf.database.utils.isTransientConflict
import io.r2dbc.spi.IsolationLevel
import kotlinx.coroutines.delay
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.r2dbc.transactions.inTopLevelSuspendTransaction
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Runs [block] in a new top-level transaction on this database and runs it again, in a new
 * transaction, when the database rolled it back for a conflict with a concurrent transaction.
 *
 * Only a transient conflict ([isTransientConflict]: deadlock, serialization failure) starts a new
 * attempt, after a short random delay that grows with each attempt. Exposed's own retries are
 * switched off for these transactions, so any other failure reaches the caller after one attempt.
 * [block] can therefore run up to [maxAttempts] times; side effects outside the database must
 * tolerate that.
 *
 * ```
 * database.retryingTransaction {
 *     Accounts.update({ Accounts.id eq from }) { it[Accounts.balance] = Accounts.balance - amount }
 *     Accounts.update({ Accounts.id eq to }) { it[Accounts.balance] = Accounts.balance + amount }
 * }
 * ```
 *
 * @param isolation the isolation level of each attempt
 * @param maxAttempts how often [block] runs at most; at least 1
 * @return the result of the attempt that committed
 * @throws IllegalArgumentException if [maxAttempts] is less than 1
 * @throws IllegalStateException if called inside an open transaction, whose rollback this function
 * cannot repeat
 */
suspend fun <T> R2dbcDatabase.retryingTransaction(
    isolation: IsolationLevel = IsolationLevel.READ_COMMITTED,
    maxAttempts: Int = 3,
    block: suspend R2dbcTransaction.() -> T,
): T {
    require(maxAttempts >= 1) { "maxAttempts must be at least 1, was $maxAttempts" }
    check(TransactionManager.currentOrNull() == null) {
        "retryingTransaction must not run inside an open transaction; it owns its transaction"
    }

    var attempt = 1

    while (true) {
        try {
            return singleAttempt(isolation, block)
        } catch (exception: Exception) {
            if (attempt == maxAttempts || !exception.isTransientConflict()) throw exception
        }

        delay(retryDelay(attempt))
        attempt++
    }
}

private suspend fun <T> R2dbcDatabase.singleAttempt(
    isolation: IsolationLevel,
    block: suspend R2dbcTransaction.() -> T,
): T {
    return inTopLevelSuspendTransaction(db = this, transactionIsolation = isolation) {
        maxAttempts = 1
        minRetryDelay = 0
        maxRetryDelay = 0
        block()
    }
}

/**
 * A random delay of up to [RETRY_DELAY_STEP] times [attempt], so that the transactions of one
 * conflict do not meet again on their next attempt.
 */
private fun retryDelay(attempt: Int): Duration {
    return Random.nextLong(RETRY_DELAY_STEP.inWholeMilliseconds * attempt + 1).milliseconds
}

private val RETRY_DELAY_STEP = 25.milliseconds
