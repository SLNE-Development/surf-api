@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js.api

import kotlinx.js.JsPlainObject

/**
 * Options of `listen`.
 */
@JsExport
@JsPlainObject
external interface ListenOptions {
    /**
     * Which listeners receive an event: `ALL_INSTANCES` (default) or `ANY_INSTANCE`.
     *
     * With `ALL_INSTANCES` every listener receives every event. With `ANY_INSTANCE` each event goes to one of the
     * clients with the same client name, and every `ANY_INSTANCE` listener of that client for the event receives it.
     * Other values are rejected.
     */
    val recipients: String?

    /**
     * If `true`, skips the events this connection published. Requires `ALL_INSTANCES`.
     */
    val ignoreFromThisClient: Boolean?
}
