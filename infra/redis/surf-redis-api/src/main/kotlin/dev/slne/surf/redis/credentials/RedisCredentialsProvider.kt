package dev.slne.surf.redis.credentials

import dev.slne.surf.redis.util.InternalRedisAPI
import dev.slne.surf.surfapi.core.api.util.requiredService
import org.redisson.misc.RedisURI

@InternalRedisAPI
interface RedisCredentialsProvider {

    fun redisURI(): RedisURI

    companion object {
        val instance = requiredService<RedisCredentialsProvider>()
    }
}