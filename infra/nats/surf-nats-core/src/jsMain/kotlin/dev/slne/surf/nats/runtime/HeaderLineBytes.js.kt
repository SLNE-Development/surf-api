package dev.slne.surf.nats.runtime

import dev.slne.surf.nats.InternalNatsApi

// nats.js writes a header line as `\r\nname: value`
@InternalNatsApi
internal actual val HEADER_LINE_SEPARATOR_BYTES: Int = 4
