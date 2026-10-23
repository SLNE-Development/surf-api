@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js

/**
 * Failure of a service call; also thrown directly when JetStream rejects a persistent publish.
 */
@JsExport
open class NatsRpcError internal constructor(message: String, cause: Throwable? = null) : Throwable(message, cause)

@JsExport
class NatsRemoteError internal constructor(
    val type: String,
    val remoteMessage: String,
) : NatsRpcError("$type: $remoteMessage")

@JsExport
class NatsNoRespondersError internal constructor(
    val subject: String,
) : NatsRpcError("No responders for $subject")

/**
 * No response, PubAck or publish flush arrived within [timeoutMs].
 */
@JsExport
class NatsCallTimeoutError internal constructor(
    val subject: String,
    val timeoutMs: Int,
) : NatsRpcError("No response on $subject within $timeoutMs ms")
