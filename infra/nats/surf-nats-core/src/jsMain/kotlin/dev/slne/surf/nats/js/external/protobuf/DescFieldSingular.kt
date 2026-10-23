@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

// fieldKind scalar, enum or message
internal external interface DescFieldSingular : DescField {
    // The proto [default] of a scalar or enum field; undefined without one and for message fields
    fun getDefaultValue(): Any?
}
