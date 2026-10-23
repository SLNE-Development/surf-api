@file:JsModule("@nats-io/jetstream")

package dev.slne.surf.nats.js.external.nats.jetstream

// Partial<StreamConfig>; durations in nanoseconds
@Suppress("PropertyName")
internal external interface StreamConfig {
    var name: String
    var subjects: Array<String>?
    var retention: String?
    var storage: String?
    var allow_msg_ttl: Boolean?
    var max_age: Double?
    var duplicate_window: Double?
    var no_ack: Boolean?
}
