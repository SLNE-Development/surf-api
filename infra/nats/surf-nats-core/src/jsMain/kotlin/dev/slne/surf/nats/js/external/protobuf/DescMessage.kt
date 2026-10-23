@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

import dev.slne.surf.nats.js.MessageSchema

// A GenMessage is passed to the exported API as MessageSchema.
internal external interface DescMessage : AnyDesc, MessageSchema {
    override val typeName: String
    val name: String
    val file: DescFile
    val fields: Array<DescField>
    val oneofs: Array<DescOneof>
}
