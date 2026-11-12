@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

import dev.slne.surf.nats.js.api.MessageSchema

/**
 * Binds `DescMessage`, which describes a message type.
 *
 * A `GenMessage`, the schema that protoc-gen-es generates, is a `DescMessage` and is passed to the exported API as
 * [MessageSchema].
 */
internal external interface DescMessage : AnyDesc, MessageSchema {
    override val typeName: String
    val name: String
    val file: DescFile
    val fields: Array<DescField>
    val oneofs: Array<DescOneof>
    val nestedMessages: Array<DescMessage>
}
