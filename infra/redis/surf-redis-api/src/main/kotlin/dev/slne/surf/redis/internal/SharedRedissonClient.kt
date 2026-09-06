package dev.slne.surf.redis.internal

import dev.slne.surf.redis.util.InternalRedisAPI
import org.redisson.api.RedissonClient
import org.redisson.api.RedissonReactiveClient
import org.redisson.codec.BaseEventCodec

@InternalRedisAPI
class SharedRedissonClient internal constructor(
    val key: RedissonConnectionKey,
    val redisson: RedissonClient,
    val redissonReactive: RedissonReactiveClient,
    val redisOsType: BaseEventCodec.OSType?,
) {
    override fun toString(): String = "SharedRedissonClient($key)"
}
