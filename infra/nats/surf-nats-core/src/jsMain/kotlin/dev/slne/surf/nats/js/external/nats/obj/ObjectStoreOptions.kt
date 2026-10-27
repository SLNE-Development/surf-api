@file:JsModule("@nats-io/obj")

package dev.slne.surf.nats.js.external.nats.obj

import kotlinx.js.JsPlainObject

// Partial<ObjectStoreOptions>
@JsPlainObject
internal external interface ObjectStoreOptions {
    val description: String?

    // Nanos
    val ttl: Double?

    // StorageType: file | memory
    val storage: String?
    val replicas: Int?
}
