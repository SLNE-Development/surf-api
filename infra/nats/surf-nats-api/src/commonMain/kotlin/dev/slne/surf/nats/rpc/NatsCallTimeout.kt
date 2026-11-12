package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.internal.NatsTimeoutElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import kotlin.time.Duration

/**
 * Runs [block] with [timeout] as the call timeout of every service call made in it.
 *
 * The override takes precedence over the `call_timeout_seconds` option of the contract, which defaults to five
 * seconds and to sixty seconds for persistent methods. It applies to calls in [block] and in coroutines that inherit
 * its context. [block] itself runs without a time limit. What the timeout bounds depends on the method, as described
 * in [Recipients].
 *
 * ```kotlin
 * val answer = withNatsCallTimeout(2.seconds) {
 *     finder.find(FindRequest("spawn"))
 * }
 * ```
 *
 * @return the result of [block].
 * @throws IllegalArgumentException if [timeout] is not positive and finite. Also thrown by a persistent call without
 * a result in [block] when [timeout] exceeds the two-minute duplicate window.
 */
public suspend fun <T> withNatsCallTimeout(
    timeout: Duration,
    block: suspend CoroutineScope.() -> T
): T {
    require(timeout.isPositive() && timeout.isFinite()) { "NATS timeout must be positive and finite, was $timeout" }
    return withContext(NatsTimeoutElement(timeout), block)
}