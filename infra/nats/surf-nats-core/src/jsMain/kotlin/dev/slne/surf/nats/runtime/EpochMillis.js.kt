package dev.slne.surf.nats.runtime

import dev.slne.surf.nats.InternalNatsApi
import kotlin.js.Date

@InternalNatsApi
internal actual fun epochMillis(): Long = Date.now().toLong()
