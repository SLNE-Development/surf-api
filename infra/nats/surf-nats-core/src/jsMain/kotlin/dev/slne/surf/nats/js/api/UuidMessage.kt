@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js.api

import js.buffer.ArrayBuffer
import js.typedarrays.Uint8Array
import kotlinx.js.JsPlainObject

/**
 * A `surf.types.v1.Uuid` message: 16 bytes, most significant half first (big-endian), as the JVM writes it.
 */
@JsExport
@JsPlainObject
external interface UuidMessage {
    @Suppress("PropertyName")
    val `$typeName`: String
    val value: Uint8Array<ArrayBuffer>
}
