@file:JsModule("@nats-io/obj")

package dev.slne.surf.nats.js.external.nats.obj

import kotlinx.js.JsPlainObject

@JsPlainObject
internal external interface ObjectStoreMeta {
    val name: String
    val description: String?
}
