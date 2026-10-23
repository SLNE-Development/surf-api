@file:JsModule("@nats-io/jetstream")

package dev.slne.surf.nats.js.external.nats.jetstream

// JetStreamManagerOptions
internal external interface JetStreamOptions {
    var apiPrefix: String?
    var timeout: Int?
    var domain: String?
    var checkAPI: Boolean?
}
