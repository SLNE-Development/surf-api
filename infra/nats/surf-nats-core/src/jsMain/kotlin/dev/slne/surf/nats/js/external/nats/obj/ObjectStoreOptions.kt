@file:JsModule("@nats-io/obj")

package dev.slne.surf.nats.js.external.nats.obj

import kotlinx.js.JsPlainObject

/**
 * Binds `Partial<ObjectStoreOptions>`.
 */
@JsPlainObject
internal external interface ObjectStoreOptions {
    val description: String?

    /**
     * Time to live of the entries in nanoseconds.
     */
    val ttl: Double?

    /**
     * `StorageType` of the underlying stream: `file` or `memory`.
     */
    val storage: String?
    val replicas: Int?
}
