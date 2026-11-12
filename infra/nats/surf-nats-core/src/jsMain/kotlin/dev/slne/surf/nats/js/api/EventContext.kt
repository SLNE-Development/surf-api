@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js.api

import kotlinx.js.JsPlainObject

/**
 * Metadata of a received event, passed to the handler of `listen`.
 */
@JsExport
@JsPlainObject
external interface EventContext {
    /**
     * The publisher's origin, `<client name>@<instanceId>`, or `null` if the publisher sent none.
     */
    val origin: String?

    /**
     * `true` if this connection published the event. Another client counts as foreign, even with the same name.
     */
    val isFromThisClient: Boolean

    /**
     * The publisher's `Surf-Event-Id`, which identifies the logical event, or `null` if the publisher sent none.
     */
    val eventId: String?
}
