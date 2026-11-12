@file:JsModule("@nats-io/obj")

package dev.slne.surf.nats.js.external.nats.obj

internal external interface ObjectInfo {
    val name: String
    val bucket: String
    val nuid: String
    val size: Double
    val chunks: Int
    val digest: String
    val deleted: Boolean
    val revision: Int
}
