@file:JsModule("@nats-io/obj")

package dev.slne.surf.nats.js.external.nats.obj

internal external interface PurgeResponse {
    val success: Boolean
    val purged: Int
}
