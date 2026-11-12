@file:JsModule("@nats-io/transport-node")

package dev.slne.surf.nats.js.external.nats.core

import js.typedarrays.Uint8Array

internal external interface Msg {
    val subject: String
    val reply: String?
    val data: Uint8Array<*>
    val headers: MsgHdrs?
    fun respond(payload: Uint8Array<*> = definedExternally, opts: NatsPublishOptions = definedExternally): Boolean
}
