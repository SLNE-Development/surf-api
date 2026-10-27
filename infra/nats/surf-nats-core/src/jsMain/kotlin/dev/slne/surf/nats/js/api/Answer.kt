@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js.api

import kotlinx.js.JsPlainObject

@JsExport
@JsPlainObject
external interface Answer {
    val known: Boolean
    val value: ProtoMessage?
}
