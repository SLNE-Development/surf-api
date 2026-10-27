package dev.slne.surf.nats.js.interop

import dev.slne.surf.nats.js.api.MessageSchema
import dev.slne.surf.nats.js.api.MethodSchema
import dev.slne.surf.nats.js.api.ProtoMessage
import dev.slne.surf.nats.js.external.protobuf.DescMessage
import dev.slne.surf.nats.js.external.protobuf.DescMethod
import dev.slne.surf.nats.js.external.protobuf.isMessage

internal fun MessageSchema.asDesc(): DescMessage {
    require(requireFromJs(this, "schema").kind == "message") { "Expected a protobuf-es message schema (GenMessage)" }
    return unsafeCast<DescMessage>()
}

internal fun MethodSchema.asDesc(): DescMethod {
    require(requireFromJs(this, "method").kind == "rpc") { "Expected a protobuf-es service method (Service.method.x)" }
    return unsafeCast<DescMethod>()
}

internal fun messageOf(schema: DescMessage, value: Any?): ProtoMessage? {
    return if (isMessage(value, schema)) value.unsafeCast<ProtoMessage>() else null
}
