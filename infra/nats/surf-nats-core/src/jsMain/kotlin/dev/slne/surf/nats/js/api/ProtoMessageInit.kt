@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js.api

/**
 * A protobuf-es message or an init object for `create()`; init objects have no `$typeName`.
 */
@JsExport
external interface ProtoMessageInit {
    @Suppress("PropertyName")
    val `$typeName`: String?
}
