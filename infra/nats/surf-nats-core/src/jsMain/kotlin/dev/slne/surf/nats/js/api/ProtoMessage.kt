@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js.api

/**
 * A protobuf-es message, identified by its full type name in `$typeName`.
 */
@JsExport
external interface ProtoMessage : ProtoMessageInit {
    override val `$typeName`: String
}
