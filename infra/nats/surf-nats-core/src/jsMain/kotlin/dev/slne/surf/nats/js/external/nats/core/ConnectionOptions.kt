@file:JsModule("@nats-io/transport-node")

package dev.slne.surf.nats.js.external.nats.core

// ConnectionOptions (NodeConnectionOptions) of the options that surf-nats sets.
internal external interface ConnectionOptions {
    // Array<string> | string; surf-nats always passes an array.
    var servers: Array<String>?
    var name: String?
    var timeout: Int?
    var maxReconnectAttempts: Int?
    var pingInterval: Int?
    var token: String?
    var user: String?
    var pass: String?
    var authenticator: Authenticator?
}
