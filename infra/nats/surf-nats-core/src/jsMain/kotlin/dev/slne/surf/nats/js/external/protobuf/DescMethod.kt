@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

import dev.slne.surf.nats.js.api.MethodSchema

// Service.method.x is passed to the exported API as MethodSchema.
internal external interface DescMethod : AnyDesc, MethodSchema {
    override val name: String
    val localName: String

    // unary | server_streaming | client_streaming | bidi_streaming
    val methodKind: String
    val parent: DescService
    val input: DescMessage
    val output: DescMessage
}
