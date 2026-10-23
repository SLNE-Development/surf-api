package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.internal.NatsTimeoutElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import kotlin.time.Duration

/**
 * Overrides the timeout of every service call made in [block].
 *
 * Takes precedence over the contract's call timeout without imposing a timeout on [block] itself.
 * Overrides exceeding two minutes are rejected when a persistent call without a result is made.
 *
 * @throws IllegalArgumentException if [timeout] is not positive and finite, or a persistent call
 * without a result in [block] uses a timeout exceeding two minutes.
 */
public suspend fun <T> withNatsCallTimeout(
    timeout: Duration,
    block: suspend CoroutineScope.() -> T
): T {
    require(timeout.isPositive() && timeout.isFinite()) { "NATS timeout must be positive and finite, was $timeout" }
    return withContext(NatsTimeoutElement(timeout), block)
}