@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js.api

import kotlinx.js.JsPlainObject

@JsExport
@JsPlainObject
external interface ListenOptions {
    val owner: String?
    val recipients: String?
    val ignoreFromThisClient: Boolean?
}
