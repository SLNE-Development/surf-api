@file:JsModule("@nats-io/jetstream")

package dev.slne.surf.nats.js.external.nats.jetstream

import kotlinx.js.JsPlainObject

// Expires & Bind
@JsPlainObject
internal external interface NextOptions {
    val expires: Int?
}
