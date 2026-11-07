@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

internal external interface DescFieldScalarList : DescFieldList {
    // ScalarType of the elements
    val scalar: Int
    val longAsString: Boolean
}
