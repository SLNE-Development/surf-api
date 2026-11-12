@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js.api

import kotlinx.js.JsPlainObject

/**
 * Options of `publish`.
 */
@JsExport
@JsPlainObject
external interface PublishOptions {
    /**
     * The ID of the logical event, sent as `Surf-Event-Id`: 1 to 128 visible ASCII characters, without spaces.
     *
     * Defaults to a random UUID. Listeners that deduplicate by event ID treat publishes with the same ID as one
     * event, so reuse the ID only when you publish the same event again.
     */
    val eventId: String?
}
