@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

internal external interface BinaryReadOptions {
    var readUnknownFields: Boolean?
    var recursionLimit: Int?
}
