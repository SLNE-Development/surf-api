@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js.api

@JsExport
external interface MessageSchema {
    val kind: String
    val typeName: String
}
