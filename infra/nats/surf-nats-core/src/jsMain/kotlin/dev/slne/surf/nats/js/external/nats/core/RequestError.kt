@file:JsModule("@nats-io/transport-node")

package dev.slne.surf.nats.js.external.nats.core

import js.errors.ErrorOptions

internal external class RequestError(
    message: String = definedExternally,
    options: ErrorOptions = definedExternally
) : Throwable {
    fun isNoResponders(): Boolean
}
