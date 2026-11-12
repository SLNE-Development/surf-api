@file:JsModule("@nats-io/transport-node")

package dev.slne.surf.nats.js.external.nats.core

import js.errors.ErrorOptions

/**
 * Binds `RequestError`, the error of a failed request. Its `cause` is, for example, a [TimeoutError] or a
 * [NoRespondersError].
 */
internal external class RequestError(
    message: String = definedExternally,
    options: ErrorOptions = definedExternally
) : Throwable {
    fun isNoResponders(): Boolean
}
