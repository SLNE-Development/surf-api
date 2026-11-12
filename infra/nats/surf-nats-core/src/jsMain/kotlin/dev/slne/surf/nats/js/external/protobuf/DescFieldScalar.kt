@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

/**
 * A singular field with `fieldKind` `scalar`.
 */
internal external interface DescFieldScalar : DescFieldSingular {
    /**
     * The `ScalarType`.
     */
    val scalar: Int

    /**
     * Whether 64-bit integers are represented as `string` instead of `bigint` (`jstype = JS_STRING`).
     */
    val longAsString: Boolean
}
