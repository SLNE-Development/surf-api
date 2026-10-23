@file:JsModule("@nats-io/jetstream")

package dev.slne.surf.nats.js.external.nats.jetstream

internal external interface PubAck {
    val stream: String
    val domain: String?
    val seq: Int
    val duplicate: Boolean
}
