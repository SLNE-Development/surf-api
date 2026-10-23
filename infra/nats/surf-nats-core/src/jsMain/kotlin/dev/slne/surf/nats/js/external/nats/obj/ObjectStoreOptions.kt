@file:JsModule("@nats-io/obj")

package dev.slne.surf.nats.js.external.nats.obj

// Partial<ObjectStoreOptions>
internal external interface ObjectStoreOptions {
    var description: String?

    // Nanos
    var ttl: Double?

    // StorageType: file | memory
    var storage: String?
    var replicas: Int?
}
