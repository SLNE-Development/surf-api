package dev.slne.surf.nats.js.interop

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Rejects `null` and `undefined` that JS callers pass for parameters Kotlin declares non-null.
 */
@OptIn(ExperimentalContracts::class)
internal fun <T : Any> requireFromJs(value: T?, name: String): T {
    contract {
        returns() implies (value != null)
    }
    return requireNotNull(value) { "$name is required" }
}

internal fun positiveTimeoutOrNull(timeoutMs: Int?, name: String): Duration? {
    if (timeoutMs == null) return null
    require(timeoutMs > 0) { "$name must be positive, was $timeoutMs ms" }
    return timeoutMs.milliseconds
}
