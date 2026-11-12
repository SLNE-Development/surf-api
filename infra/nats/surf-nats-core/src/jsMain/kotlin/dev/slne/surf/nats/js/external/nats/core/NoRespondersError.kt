@file:JsModule("@nats-io/transport-node")

package dev.slne.surf.nats.js.external.nats.core

import js.errors.ErrorOptions

/**
 * Binds `NoRespondersError`, which is usually the `cause` of a [RequestError] when nothing subscribes to the subject.
 */
internal external class NoRespondersError(
    subject: String,
    options: ErrorOptions = definedExternally,
) : Throwable {
    val subject: String
}
