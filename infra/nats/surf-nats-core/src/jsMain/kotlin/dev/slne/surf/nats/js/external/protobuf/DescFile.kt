@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

internal external interface DescFile : AnyDesc {
    /**
     * Name of the file without the `.proto` suffix, for example `foo/bar` for `foo/bar.proto`.
     */
    val name: String
    val dependencies: Array<DescFile>
    val extensions: Array<DescExtension>
    val messages: Array<DescMessage>
    val services: Array<DescService>

    /**
     * The `FileDescriptorProto` that the descriptor was built from.
     */
    val proto: FileDescriptorProto
}
