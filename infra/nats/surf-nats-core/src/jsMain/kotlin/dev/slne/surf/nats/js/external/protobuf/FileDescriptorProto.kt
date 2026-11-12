@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

import dev.slne.surf.nats.js.api.ProtoMessage

/**
 * Binds `FileDescriptorProto` of `google/protobuf/descriptor.proto`.
 */
internal external interface FileDescriptorProto : ProtoMessage {
    /**
     * Name of the file including the `.proto` suffix, for example `foo/bar.proto`.
     */
    val name: String
    val `package`: String

    /**
     * Every import, public ones included.
     */
    val dependency: Array<String>

    /**
     * `proto2`, `proto3` or `editions`. Empty for `proto2`.
     */
    val syntax: String
    val sourceCodeInfo: SourceCodeInfo?
}
