package dev.slne.surf.database.reliability.internal

import io.r2dbc.spi.IsolationLevel
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.transactions.inTopLevelSuspendTransaction

/**
 * Runs [block] in a new top-level READ COMMITTED transaction on this database, independent of any
 * transaction of the caller.
 *
 * Exposed's own retries are disabled, so a failure reaches the caller after one attempt and every retry is decided by
 * the reliability stores.
 */
internal suspend fun <T> R2dbcDatabase.readCommittedTransaction(
    block: suspend R2dbcTransaction.() -> T
): T = inTopLevelSuspendTransaction(
    db = this,
    transactionIsolation = IsolationLevel.READ_COMMITTED,
) {
    maxAttempts = 1
    minRetryDelay = 0
    maxRetryDelay = 0
    block()
}
