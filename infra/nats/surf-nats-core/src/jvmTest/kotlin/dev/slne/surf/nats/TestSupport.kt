package dev.slne.surf.nats

import dev.slne.surf.nats.connection.NatsConnectionManager
import io.nats.client.JetStreamManagement
import io.nats.client.Message
import io.nats.client.RequestFailureMessage
import io.nats.client.impl.Headers
import kotlinx.coroutines.delay
import kotlin.test.fail
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Polls [condition] every 50 ms and fails the test with [message] if it does not hold within [timeout].
 */
suspend fun eventually(
    message: String,
    timeout: Duration = 10.seconds,
    condition: suspend () -> Boolean,
) {
    val start = TimeSource.Monotonic.markNow()
    while (!condition()) {
        if (start.elapsedNow() > timeout) fail("$message (waited $timeout)")
        delay(50.milliseconds)
    }
}

fun jsm(): JetStreamManagement = NatsConnectionManager.connection().jetStreamManagement()

fun messageCount(stream: String): Long = jsm().getStreamInfo(stream).streamState.msgCount

/**
 * Sends a synchronous request on the shared connection and fails the test if no reply arrives.
 *
 * The shared connection uses `advancedRequestBehavior`, so jnats reports a missing reply as a
 * [RequestFailureMessage] instead of `null`.
 */
fun requestReply(
    subject: String,
    headers: Headers?,
    body: ByteArray,
    timeout: java.time.Duration = java.time.Duration.ofSeconds(5),
): Message {
    val reply = NatsConnectionManager.connection().request(subject, headers, body, timeout)
    if (reply == null || reply is RequestFailureMessage) {
        fail("No reply on $subject: ${(reply as? RequestFailureMessage)?.reason ?: "timeout"}")
    }
    return reply
}
