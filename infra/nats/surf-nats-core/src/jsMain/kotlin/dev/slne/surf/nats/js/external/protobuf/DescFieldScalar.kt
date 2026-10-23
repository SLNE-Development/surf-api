@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

internal external interface DescFieldScalar : DescFieldSingular {
    // ScalarType
    val scalar: Int
    val longAsString: Boolean
}
