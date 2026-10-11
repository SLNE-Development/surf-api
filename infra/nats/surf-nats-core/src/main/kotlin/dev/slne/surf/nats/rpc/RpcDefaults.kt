package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.InternalNatsApi
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@InternalNatsApi
internal object RpcDefaults {
    val CORE_TIMEOUT = 5.seconds
    const val MAX_ERROR_MESSAGE = 4096

    val HEARTBEAT_INTERVAL = 5.seconds
    val PRESENCE_TTL = 15.seconds
    val PRESENCE_WARMUP = 1.seconds

    val JETSTREAM_TIMEOUT = 60.seconds
    val PERSIST_MAX_AGE = 7.days
    val DUPLICATE_WINDOW = 2.minutes
    val ACK_SYNC_TIMEOUT = 5.seconds
    @Volatile
    internal var ACK_WAIT = 30.seconds // var for tests

    const val MAX_DELIVER = 5

    @Volatile
    internal var NAK_BACKOFF = listOf(1.seconds, 5.seconds, 30.seconds, 2.minutes) // var for tests
}