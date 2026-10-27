@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js.api

import kotlinx.js.JsPlainObject

@JsExport
@JsPlainObject
external interface ConnectOptions {
    val name: String
    val servers: Array<String>?
    val token: String?
    val user: String?
    val pass: String?
    val credsFile: String?
    val connectTimeoutMs: Int?
}
