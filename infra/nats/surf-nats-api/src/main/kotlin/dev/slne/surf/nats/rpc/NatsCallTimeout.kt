package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.internal.NatsTimeoutElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import kotlin.time.Duration

/**
 * Sets the default caller-side timeout for service calls.
 *
 * A method's annotation takes precedence over its interface's annotation. The default is five
 * seconds for non-persistent methods and 60 seconds for persistent methods. [withNatsCallTimeout]
 * takes precedence over this annotation.
 * The timeout limits waiting for a response or storage confirmation, rather than the total time
 * spent serializing and transferring payloads.
 *
 * Persistent methods without a result are limited to the two-minute deduplication window, because
 * their timeout only covers the storage confirmation. Persistent methods with a result wait for the
 * storage confirmation for at most two minutes and for the response until the full timeout.
 * A call whose arguments or result are moved to the object store fails if its timeout exceeds ten
 * minutes, the lifetime of transient payloads: arguments with [IllegalArgumentException], a result
 * with [NatsRemoteException].
 *
 * @property seconds the timeout in seconds. Must be positive.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
public annotation class NatsCallTimeout(val seconds: Long)

/**
 * Overrides the timeout of every service call made in [block].
 *
 * Takes precedence over [NatsCallTimeout] without imposing a timeout on [block] itself.
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