@file:JsModule("@nats-io/jetstream")

package dev.slne.surf.nats.js.external.nats.jetstream

internal external interface StreamInfo {
    val config: StreamConfig
    val state: StreamState
}
