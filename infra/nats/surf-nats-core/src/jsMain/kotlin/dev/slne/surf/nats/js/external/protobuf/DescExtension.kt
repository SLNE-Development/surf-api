@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

internal external interface DescExtension : AnyDesc {
    val typeName: String
    val name: String
    val number: Int

    /**
     * One of `scalar`, `enum`, `message` and `list`.
     */
    val fieldKind: String
    val extendee: DescMessage
    val file: DescFile

    /**
     * Set for message-typed extensions.
     */
    val message: DescMessage?
}
