@file:JsModule("@nats-io/transport-node")

package dev.slne.surf.nats.js.external.nats.core

import js.typedarrays.Uint8Array

internal external interface MsgHdrs {
    // Status code of a status message (503 = no responders), 0 for regular messages.
    val code: Int
    val description: String

    // Returns "" for an absent header.
    fun get(k: String): String
    fun set(k: String, v: String)
    fun has(k: String): Boolean
    fun delete(k: String)
    fun keys(): Array<String>

    // Member of MsgHdrsImpl, the class of every MsgHdrs that nats.js creates; not part of the MsgHdrs interface.
    fun encode(): Uint8Array<*>
}
