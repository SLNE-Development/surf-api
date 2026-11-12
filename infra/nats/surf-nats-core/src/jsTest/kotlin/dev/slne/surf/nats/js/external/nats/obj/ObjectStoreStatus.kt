@file:JsModule("@nats-io/obj")

package dev.slne.surf.nats.js.external.nats.obj

// ttl in nanoseconds; storage: file | memory
internal external interface ObjectStoreStatus {
    val bucket: String
    val ttl: Double
    val storage: String
}
