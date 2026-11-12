@file:JsModule("@nats-io/jetstream")

package dev.slne.surf.nats.js.external.nats.jetstream

/**
 * Binds `PubAck`, the acknowledgement of a JetStream publish. `duplicate` is set when the stream rejected the message
 * as a duplicate of an earlier `msgID`.
 */
internal external interface PubAck {
    val stream: String
    val domain: String?
    val seq: Int
    val duplicate: Boolean
}
