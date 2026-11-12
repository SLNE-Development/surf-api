package dev.slne.surf.nats.js.codec

import js.typedarrays.Uint8Array
import web.encoding.TextDecoder

internal fun Uint8Array<*>.decodeToString(): String = TextDecoder().decode(this)
