@file:JsModule("@nats-io/transport-node")

package dev.slne.surf.nats.js.external.nats.core

/**
 * Binds the members of `ServerInfo` that surf-nats reads, named as in the `INFO` message of the server.
 */
internal external interface ServerInfo {
    /**
     * Largest payload in bytes that the server accepts.
     */
    @Suppress("PropertyName")
    val max_payload: Int
    val jetstream: Boolean?
    val version: String
}
