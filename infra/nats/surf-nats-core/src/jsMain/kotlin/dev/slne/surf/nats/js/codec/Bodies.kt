package dev.slne.surf.nats.js.codec

import js.buffer.ArrayBuffer
import js.typedarrays.Uint8Array
import web.encoding.TextDecoder

internal fun emptyBody(): Uint8Array<*> = Uint8Array<ArrayBuffer>(0)

internal fun Uint8Array<*>.decodeUtf8(): String = TextDecoder().decode(this)
