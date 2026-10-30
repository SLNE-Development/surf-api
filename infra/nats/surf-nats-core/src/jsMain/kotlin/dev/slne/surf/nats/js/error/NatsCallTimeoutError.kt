@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js.error

import dev.slne.surf.nats.rpc.NatsRpcMessages
import kotlin.time.Duration.Companion.milliseconds

/**
 * No response, PubAck or publish flush arrived within [timeoutMs].
 */
@JsExport
class NatsCallTimeoutError internal constructor(
    val subject: String,
    val timeoutMs: Int,
) : NatsRpcError(NatsRpcMessages.callTimeout(subject, timeoutMs.milliseconds))
