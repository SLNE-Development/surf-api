package dev.slne.surf.database.reliability.internal

import dev.slne.surf.database.DatabaseApi
import io.r2dbc.spi.IsolationLevel
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.transactions.inTopLevelSuspendTransaction

/**
 * Runs [block] in a new top-level READ COMMITTED transaction on [DatabaseApi.database], independent of any
 * transaction of the caller.
 *
 * Exposed's own retries are disabled, so a failure reaches the caller after one attempt and every retry is decided by
 * the reliability stores.
 */
internal suspend fun <T> DatabaseApi.readCommittedTransaction(
    block: suspend R2dbcTransaction.() -> T
): T = inTopLevelSuspendTransaction(
    db = database,
    transactionIsolation = IsolationLevel.READ_COMMITTED,
) {
    maxAttempts = 1
    minRetryDelay = 0
    maxRetryDelay = 0
    block()
}
