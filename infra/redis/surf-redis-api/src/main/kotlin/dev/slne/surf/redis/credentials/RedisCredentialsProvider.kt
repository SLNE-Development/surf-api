package dev.slne.surf.redis.credentials

import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.redis.util.InternalRedisAPI
import org.redisson.misc.RedisURI

@InternalRedisAPI
interface RedisCredentialsProvider {

    fun redisURI(): RedisURI

    companion object : RedisCredentialsProvider by provider {
        val INSTANCE get() = provider
    }
}

private val provider = requiredService<RedisCredentialsProvider>()