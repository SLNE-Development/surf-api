package dev.slne.surf.nats.rpc.payload

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.rpc.RpcDefaults
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@InternalNatsApi
internal enum class PayloadBucket(val bucketName: String, val ttl: Duration) {
    TRANSIENT("surf-nats-payloads", 10.minutes),
    PERSISTENT("surf-nats-persistent-payloads", RpcDefaults.PERSIST_MAX_AGE),
}