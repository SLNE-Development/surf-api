@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

/**
 * A field with `fieldKind` `map`.
 */
internal external interface DescFieldMap : DescField {
    /**
     * Kind of the values: `scalar`, `enum` or `message`.
     */
    val mapKind: String

    /**
     * `ScalarType` of the key.
     */
    val mapKey: Int
}
