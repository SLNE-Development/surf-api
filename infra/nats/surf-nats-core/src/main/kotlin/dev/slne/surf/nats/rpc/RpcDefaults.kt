package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.InternalNatsApi
import kotlin.time.Duration.Companion.seconds

@InternalNatsApi
internal object RpcDefaults {
    val CORE_TIMEOUT = 5.seconds
    const val MAX_ERROR_MESSAGE = 4096
}