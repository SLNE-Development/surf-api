package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.InternalNatsApi
import kotlin.time.Duration.Companion.seconds

@InternalNatsApi
internal object RpcDefaults {
    val CORE_TIMEOUT = 5.seconds
    const val MAX_ERROR_MESSAGE = 4096

    val HEARTBEAT_INTERVAL = 5.seconds
    val PRESENCE_TTL = 15.seconds
    val PRESENCE_WARMUP = 1.seconds
}