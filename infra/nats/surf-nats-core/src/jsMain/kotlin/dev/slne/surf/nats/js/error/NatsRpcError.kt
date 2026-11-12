@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js.error

/**
 * Base class of the errors a failed service call rejects with.
 *
 * Thrown directly when JetStream rejects the request of a persistent method, for example because its stream does
 * not exist. Invalid arguments, options and a closed client fail with other errors.
 */
@JsExport
open class NatsRpcError internal constructor(message: String, cause: Throwable? = null) : Throwable(message, cause)
