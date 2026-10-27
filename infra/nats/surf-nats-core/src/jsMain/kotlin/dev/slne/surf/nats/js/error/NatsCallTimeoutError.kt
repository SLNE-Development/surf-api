@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js.error

/**
 * No response, PubAck or publish flush arrived within [timeoutMs].
 */
@JsExport
class NatsCallTimeoutError internal constructor(
    val subject: String,
    val timeoutMs: Int,
) : NatsRpcError("No response on $subject within $timeoutMs ms")
