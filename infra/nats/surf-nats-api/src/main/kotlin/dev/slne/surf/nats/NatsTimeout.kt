package dev.slne.surf.nats

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class NatsTimeout(val seconds: Long)

@InternalNatsApi
class NatsTimeoutElement(val timeout: Duration) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<NatsTimeoutElement>
}

// TODO: 08.10.2026 17:46 - reference somewhere in the kdocs

suspend fun <T> withNatsTimeout(
    timeout: Duration,
    block: suspend CoroutineScope.() -> T
): T {
    require(timeout.isPositive()) { "NATS timeout must be positive, was $timeout" }
    return withContext(NatsTimeoutElement(timeout), block)
}