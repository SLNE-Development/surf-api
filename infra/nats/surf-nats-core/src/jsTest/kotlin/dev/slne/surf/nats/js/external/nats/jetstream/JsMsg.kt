@file:JsModule("@nats-io/jetstream")

package dev.slne.surf.nats.js.external.nats.jetstream

import dev.slne.surf.nats.js.external.nats.core.MsgHdrs
import js.typedarrays.Uint8Array

internal external interface JsMsg {
    val subject: String
    val seq: Int
    val headers: MsgHdrs?
    val data: Uint8Array<*>
    fun ack()
}
