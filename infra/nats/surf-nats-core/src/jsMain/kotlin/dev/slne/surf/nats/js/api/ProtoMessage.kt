@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js.api

@JsExport
external interface ProtoMessage : ProtoMessageInit {
    override val `$typeName`: String
}
