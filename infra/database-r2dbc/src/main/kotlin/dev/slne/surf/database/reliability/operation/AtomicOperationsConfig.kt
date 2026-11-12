package dev.slne.surf.database.reliability.operation

import java.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Settings of [AtomicOperations].
 *
 * @property retention How long a stored reply is kept. A repeated request after this period executes again, so it
 * must exceed the longest time a request can be redelivered or retried.
 * @property lockTimeout How long an execution waits for a concurrent execution of the same key before it fails with
 * [AtomicOperationInProgressException]. PostgreSQL truncates it to whole milliseconds, MariaDB rounds it up to whole
 * seconds.
 * @property maxAttempts Executions per call, counting retries after deadlocks, serialization failures and claims
 * that vanished concurrently. Deleting an expired reply does not consume an attempt.
 * @property clock Source of `created_at` and `expires_at`. All processes sharing the tables need synchronized clocks.
 * @throws IllegalArgumentException if [retention] is not positive, [lockTimeout] is outside 1 ms to
 * [Int.MAX_VALUE] ms (about 24.8 days) or [maxAttempts] is below 1
 */
data class AtomicOperationsConfig(
    val retention: Duration = DEFAULT_RETENTION,
    val lockTimeout: Duration = DEFAULT_LOCK_TIMEOUT,
    val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    val clock: Clock = Clock.systemUTC(),
) {
    init {
        require(retention.isPositive()) { "retention must be positive, was $retention" }
        require(lockTimeout in MIN_LOCK_TIMEOUT..MAX_LOCK_TIMEOUT) {
            "lockTimeout must be between $MIN_LOCK_TIMEOUT and $MAX_LOCK_TIMEOUT, was $lockTimeout"
        }
        require(maxAttempts >= 1) { "maxAttempts must be at least 1, was $maxAttempts" }
    }

    companion object {
        /**
         * Default [retention].
         */
        val DEFAULT_RETENTION: Duration = 14.days

        /**
         * Default [lockTimeout].
         */
        val DEFAULT_LOCK_TIMEOUT: Duration = 10.seconds

        /**
         * Default [maxAttempts].
         */
        const val DEFAULT_MAX_ATTEMPTS = 3

        /**
         * Shortest [lockTimeout]. PostgreSQL's `lock_timeout` takes whole milliseconds, and `0` disables the wait bound.
         */
        private val MIN_LOCK_TIMEOUT: Duration = 1.milliseconds

        /**
         * Longest [lockTimeout], the upper limit of PostgreSQL's `lock_timeout`.
         */
        private val MAX_LOCK_TIMEOUT: Duration = Int.MAX_VALUE.milliseconds
    }
}
