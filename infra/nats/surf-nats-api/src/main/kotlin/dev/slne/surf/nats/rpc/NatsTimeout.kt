package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.internal.NatsTimeoutElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import kotlin.time.Duration

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
public annotation class NatsTimeout(val seconds: Long)

// TODO: 08.10.2026 17:46 - reference somewhere in the kdocs

public suspend fun <T> withNatsCallTimeout(
    timeout: Duration,
    block: suspend CoroutineScope.() -> T
): T {
    require(timeout.isPositive()) { "NATS timeout must be positive, was $timeout" }
    return withContext(NatsTimeoutElement(timeout), block)
}