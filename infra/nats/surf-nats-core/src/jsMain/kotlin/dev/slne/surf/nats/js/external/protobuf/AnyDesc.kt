@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

// Descriptor shapes of descriptors.d.ts, limited to the members surf-nats reads.

internal external interface AnyDesc {
    // file | enum | enum_value | message | field | extension | oneof | service | rpc
    val kind: String
}
