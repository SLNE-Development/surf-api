@file:JsModule("@nats-io/transport-node")

package dev.slne.surf.nats.js.external.nats.core

import js.errors.ErrorOptions

internal external class NoRespondersError(
    subject: String,
    options: ErrorOptions = definedExternally,
) : Throwable {
    val subject: String
}
