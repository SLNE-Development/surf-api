@file:JsModule("@nats-io/jetstream")

package dev.slne.surf.nats.js.external.nats.jetstream

import kotlinx.js.JsPlainObject

// JetStreamManagerOptions
@JsPlainObject
internal external interface JetStreamOptions {
    val apiPrefix: String?
    val timeout: Int?
    val domain: String?
    val checkAPI: Boolean?
}
