@file:JsModule("@nats-io/transport-node")

package dev.slne.surf.nats.js.external.nats.core

internal external interface RequestOptions {
    var timeout: Int
    var headers: MsgHdrs?
    var noMux: Boolean?
    var reply: String?
}
