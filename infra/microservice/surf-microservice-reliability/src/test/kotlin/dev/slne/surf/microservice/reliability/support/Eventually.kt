package dev.slne.surf.microservice.reliability.support

import kotlinx.coroutines.delay
import kotlin.test.fail
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Polls [condition] every 50 ms and fails with [message] if it does not hold within [timeout].
 */
suspend fun eventually(message: String, timeout: Duration = 20.seconds, condition: suspend () -> Boolean) {
    val start = TimeSource.Monotonic.markNow()
    while (!condition()) {
        if (start.elapsedNow() > timeout) fail("$message (waited $timeout)")
        delay(50.milliseconds)
    }
}
