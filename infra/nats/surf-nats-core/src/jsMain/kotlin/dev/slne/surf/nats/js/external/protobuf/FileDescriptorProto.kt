@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

import dev.slne.surf.nats.js.api.ProtoMessage

internal external interface FileDescriptorProto : ProtoMessage {
    val name: String
    val `package`: String

    // Every import, public ones included
    val dependency: Array<String>

    // "proto2", "proto3" or "editions"; empty for proto2
    val syntax: String
    val sourceCodeInfo: SourceCodeInfo?
}
