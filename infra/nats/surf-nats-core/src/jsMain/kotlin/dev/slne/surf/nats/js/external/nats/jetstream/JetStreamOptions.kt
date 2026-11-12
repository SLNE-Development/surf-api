@file:JsModule("@nats-io/jetstream")

package dev.slne.surf.nats.js.external.nats.jetstream

import kotlinx.js.JsPlainObject

/**
 * Binds `JetStreamManagerOptions`, the options of `jetstream` plus `checkAPI`. `timeout` is milliseconds.
 */
@JsPlainObject
internal external interface JetStreamOptions {
    val apiPrefix: String?
    val timeout: Int?
    val domain: String?
    val checkAPI: Boolean?
}
