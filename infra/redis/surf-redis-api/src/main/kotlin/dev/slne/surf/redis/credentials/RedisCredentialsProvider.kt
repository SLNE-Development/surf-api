package dev.slne.surf.redis.credentials

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.api.spring.surfBean
import dev.slne.surf.redis.util.InternalRedisAPI
import org.redisson.misc.RedisURI

@InternalRedisAPI
interface RedisCredentialsProvider {

    fun redisURI(): RedisURI

    @InternalRedisAPI
    companion object {
        @InternalSurfApi
        val instance by surfBean<RedisCredentialsProvider>()
    }
}
