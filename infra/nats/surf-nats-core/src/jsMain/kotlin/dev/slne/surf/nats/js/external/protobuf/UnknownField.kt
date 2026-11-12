@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

import js.typedarrays.Uint8Array

/**
 * Binds `UnknownField`, a field that the schema does not know and that the message keeps as raw bytes.
 */
internal external interface UnknownField {
    val no: Int

    /**
     * The `WireType`.
     */
    val wireType: Int
    val data: Uint8Array<*>
}
