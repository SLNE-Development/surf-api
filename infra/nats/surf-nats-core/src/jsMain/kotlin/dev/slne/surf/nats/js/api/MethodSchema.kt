@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js.api

@JsExport
external interface MethodSchema {
    val kind: String
    val name: String
}
