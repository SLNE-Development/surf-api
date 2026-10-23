package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.protocol.ProtocolDefaults
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@InternalNatsApi
internal object RpcDefaults {
    val HEARTBEAT_INTERVAL = ProtocolDefaults.PRESENCE_HEARTBEAT_SECONDS.seconds
    val PRESENCE_TTL = ProtocolDefaults.PRESENCE_TTL_SECONDS.seconds
    val PRESENCE_WARMUP = 1.seconds

    val ACK_SYNC_TIMEOUT = 5.seconds
    @Volatile
    internal var ACK_WAIT = 10.seconds // var for tests
    @Volatile
    internal var FIRE_MAX_DURATION = 10.minutes // var for tests

    const val MAX_DELIVER = 5

    @Volatile
    internal var NAK_BACKOFF = listOf(1.seconds, 5.seconds, 30.seconds, 2.minutes) // var for tests
}
