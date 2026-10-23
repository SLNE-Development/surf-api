@file:JsModule("@nats-io/transport-node")

package dev.slne.surf.nats.js.external.nats.core

internal external interface ServerInfo {
    @Suppress("PropertyName")
    val max_payload: Int
    val jetstream: Boolean?
    val version: String
}
