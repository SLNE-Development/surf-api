package dev.slne.surf.redis.credentials

import dev.slne.surf.redis.util.InternalRedisAPI
import dev.slne.surf.redis.util.requiredService
import org.redisson.misc.RedisURI

@InternalRedisAPI
interface RedisCredentialsProvider {

    fun redisURI(): RedisURI

    companion object : RedisCredentialsProvider by provider {
        val INSTANCE get() = provider
    }
}

private val provider = requiredService<RedisCredentialsProvider>()