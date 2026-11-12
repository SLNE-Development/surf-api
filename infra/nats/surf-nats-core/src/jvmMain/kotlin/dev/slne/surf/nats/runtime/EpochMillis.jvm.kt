package dev.slne.surf.nats.runtime

import dev.slne.surf.nats.InternalNatsApi

@InternalNatsApi
internal actual fun epochMillis(): Long = System.currentTimeMillis()
