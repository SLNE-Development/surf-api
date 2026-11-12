@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js.api

/**
 * A protobuf-es message schema (`GenMessage`), such as the `...Schema` constants that protoc-gen-es generates.
 *
 * Only [kind] `"message"` is accepted.
 */
@JsExport
external interface MessageSchema {
    val kind: String
    val typeName: String
}
