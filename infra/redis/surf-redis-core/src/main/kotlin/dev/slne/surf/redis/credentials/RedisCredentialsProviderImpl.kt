package dev.slne.surf.redis.credentials

import com.google.auto.service.AutoService
import dev.slne.surf.redis.config.RedisConfig
import org.redisson.misc.RedisURI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@AutoService(RedisCredentialsProvider::class)
class RedisCredentialsProviderImpl : RedisCredentialsProvider {
    override fun redisURI(): RedisURI {
        val config = RedisConfig.getConfig()
        val redisURIString = buildString {
            append(RedisURI.REDIS_PROTOCOL)
            val password = config.password
            if (!password.isNullOrEmpty()) {
                append(URLEncoder.encode(password, StandardCharsets.UTF_8))
                append('@')
            }

            append(config.host)
            append(':')
            append(config.port)
        }

        return RedisURI(redisURIString)
    }
}