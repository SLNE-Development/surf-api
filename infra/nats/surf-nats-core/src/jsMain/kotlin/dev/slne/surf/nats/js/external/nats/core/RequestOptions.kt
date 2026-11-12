@file:JsModule("@nats-io/transport-node")

package dev.slne.surf.nats.js.external.nats.core

import kotlinx.js.JsPlainObject

/**
 * Binds `RequestOptions`. `timeout` is milliseconds.
 */
@JsPlainObject
internal external interface RequestOptions {
    val timeout: Int
    val headers: MsgHdrs?
    val noMux: Boolean?
    val reply: String?
}
