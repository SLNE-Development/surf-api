@file:JsModule("@nats-io/jetstream")

package dev.slne.surf.nats.js.external.nats.jetstream

@Suppress("PropertyName")
internal external interface ConsumerInfo {
    val stream_name: String
    val name: String
}
