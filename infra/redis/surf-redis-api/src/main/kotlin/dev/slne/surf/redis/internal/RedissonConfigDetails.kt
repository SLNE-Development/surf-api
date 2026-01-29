package dev.slne.surf.redis.internal

import dev.slne.surf.redis.util.InternalRedisAPI
import kotlinx.serialization.modules.SerializersModule
import org.redisson.misc.RedisURI

@InternalRedisAPI
data class RedissonConfigDetails(
    val redisURI: RedisURI,
    val serializerModule: SerializersModule
)
