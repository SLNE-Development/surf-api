@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js.api

import kotlinx.js.JsPlainObject

/**
 * Options of `call` and `callAnswer`.
 */
@JsExport
@JsPlainObject
external interface CallOptions {
    /**
     * The call timeout in milliseconds, overriding the contract's `call_timeout_seconds`. Must be positive.
     *
     * Defaults to the contract's timeout, or 5 seconds, or 60 seconds for a persistent method. A persistent method
     * without a result allows at most two minutes, the duplicate window of JetStream. Other calls have no upper limit,
     * unless the request exceeds the server's max payload: the object store keeps such a request for 10 minutes, so
     * its call rejects a longer timeout.
     */
    val timeoutMs: Int?

    /**
     * The key of one logical action, such as one submitted form: 1 to 128 visible ASCII characters.
     *
     * Only persistent methods use it. The service receives the key as the message ID of the call (`Surf-Message-Id`).
     * A service that deduplicates by message ID runs the action once and answers a repeated call with the same key and
     * an equal request, for example after a timeout, with its stored reply. Use a new key for every action.
     */
    val idempotencyKey: String?
}
