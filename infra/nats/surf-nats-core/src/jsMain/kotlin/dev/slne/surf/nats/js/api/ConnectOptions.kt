@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js.api

import kotlinx.js.JsPlainObject

@JsExport
@JsPlainObject
external interface ConnectOptions {
    val name: String

    /**
     * A comma-separated server list or an array of server URLs.
     */
    val servers: Any?
    val token: String?
    val user: String?
    val pass: String?
    val credsFile: String?
    val connectTimeoutMs: Int?
}
