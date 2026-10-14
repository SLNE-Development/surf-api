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
 * @property seconds the timeout in seconds. Must be positive and at most 120 for persistent methods.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
public annotation class NatsCallTimeout(val seconds: Long)

/**
 * Overrides the timeout of every service call made in [block].
 *
 * Takes precedence over [NatsCallTimeout] without imposing a timeout on [block] itself.
 * Overrides exceeding two minutes are rejected when a persistent call is made.
 *
 * @throws IllegalArgumentException if [timeout] is not positive and finite, or a persistent call
 * in [block] uses a timeout exceeding two minutes.
 */
public suspend fun <T> withNatsCallTimeout(
    timeout: Duration,
    block: suspend CoroutineScope.() -> T
): T {
    require(timeout.isPositive() && timeout.isFinite()) { "NATS timeout must be positive and finite, was $timeout" }
    return withContext(NatsTimeoutElement(timeout), block)
}