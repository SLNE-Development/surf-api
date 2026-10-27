@file:JsModule("@nats-io/transport-node")

package dev.slne.surf.nats.js.external.nats.core

import kotlinx.js.JsPlainObject

// ConnectionOptions (NodeConnectionOptions) of the options that surf-nats sets.
@JsPlainObject
internal external interface ConnectionOptions {
    // Array<string> | string; surf-nats always passes an array.
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
