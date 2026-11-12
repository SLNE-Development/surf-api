@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

/**
 * A list field of scalars.
 */
internal external interface DescFieldScalarList : DescFieldList {
    /**
     * `ScalarType` of the elements.
     */
    val scalar: Int

    /**
     * Whether 64-bit integers are represented as `string` instead of `bigint` (`jstype = JS_STRING`).
     */
    val longAsString: Boolean
}
