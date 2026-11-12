@file:JsModule("@nats-io/transport-node")

package dev.slne.surf.nats.js.external.nats.core

import kotlinx.js.JsPlainObject

/**
 * Binds the members of `NodeConnectionOptions` that surf-nats sets.
 *
 * `timeout` (connection handshake) and `pingInterval` are milliseconds.
 */
@JsPlainObject
internal external interface ConnectionOptions {
    /**
     * Server URLs. nats.js accepts an array or a string, surf-nats always passes an array.
     */
    val servers: Array<String>?
    val name: String?
    val timeout: Int?
    val maxReconnectAttempts: Int?
    val pingInterval: Int?
    val token: String?
    val user: String?
    val pass: String?
    val authenticator: Authenticator?
}
