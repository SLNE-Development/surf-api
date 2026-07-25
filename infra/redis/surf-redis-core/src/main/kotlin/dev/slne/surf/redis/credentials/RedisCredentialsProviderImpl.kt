package dev.slne.surf.redis.credentials

import com.google.auto.service.AutoService
import dev.slne.surf.redis.config.redisConfig
import org.redisson.misc.RedisURI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@AutoService(RedisCredentialsProvider::class)
class RedisCredentialsProviderImpl : RedisCredentialsProvider {
    override fun redisURI(): RedisURI {
        val redisURIString = buildString {
            append(RedisURI.REDIS_PROTOCOL)
            val password = redisConfig.password
            if (!password.isNullOrEmpty()) {
                append(URLEncoder.encode(password, StandardCharsets.UTF_8))
                append('@')
            }

            append(redisConfig.host)
            append(':')
            append(redisConfig.port)
        }

        return RedisURI(redisURIString)
    }
}