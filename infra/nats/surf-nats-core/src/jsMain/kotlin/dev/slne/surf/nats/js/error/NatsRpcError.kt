@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js.error

/**
 * Failure of a service call; also thrown directly when JetStream rejects a persistent publish.
 */
@JsExport
open class NatsRpcError internal constructor(message: String, cause: Throwable? = null) : Throwable(message, cause)
