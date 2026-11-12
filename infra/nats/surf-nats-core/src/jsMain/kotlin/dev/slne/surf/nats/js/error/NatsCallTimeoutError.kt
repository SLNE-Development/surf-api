@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js.error

import dev.slne.surf.nats.rpc.NatsRpcMessages
import kotlin.time.Duration.Companion.milliseconds

/**
 * Thrown when a call received no response, or for a persistent method no storage confirmation from JetStream,
 * within [timeoutMs].
 *
 * `callAnswer` never throws it: a timeout without a known answer resolves as an unknown answer.
 *
 * @property subject the subject of the called method
 * @property timeoutMs the timeout of the call in milliseconds
 */
@JsExport
class NatsCallTimeoutError internal constructor(
    val subject: String,
    val timeoutMs: Int,
) : NatsRpcError(NatsRpcMessages.callTimeout(subject, timeoutMs.milliseconds))
