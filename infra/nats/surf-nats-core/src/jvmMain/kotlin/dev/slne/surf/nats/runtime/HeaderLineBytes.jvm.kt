package dev.slne.surf.nats.runtime

import dev.slne.surf.nats.InternalNatsApi

// jnats writes a header line as `name:value\r\n`
@InternalNatsApi
internal actual val HEADER_LINE_SEPARATOR_BYTES: Int = 3
