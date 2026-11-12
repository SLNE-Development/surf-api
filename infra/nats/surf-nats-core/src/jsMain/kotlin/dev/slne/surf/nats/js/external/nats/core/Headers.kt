@file:JsModule("@nats-io/transport-node")

package dev.slne.surf.nats.js.external.nats.core

/**
 * Binds `headers`, which creates an empty header set. `code` and `description` set the status of a status message.
 */
internal external fun headers(code: Int = definedExternally, description: String = definedExternally): MsgHdrs
