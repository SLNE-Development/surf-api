@file:JsModule("@nats-io/jetstream")

package dev.slne.surf.nats.js.external.nats.jetstream

import kotlinx.js.JsPlainObject

// Partial<StreamConfig>; durations in nanoseconds
@Suppress("PropertyName")
@JsPlainObject
internal external interface StreamConfig {
    val name: String
    val subjects: Array<String>?
    val retention: String?
    val storage: String?
    val allow_msg_ttl: Boolean?
    val max_age: Double?
    val duplicate_window: Double?
    val no_ack: Boolean?
}
