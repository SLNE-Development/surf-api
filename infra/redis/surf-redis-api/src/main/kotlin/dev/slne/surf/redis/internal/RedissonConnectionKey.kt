package dev.slne.surf.redis.internal

import dev.slne.surf.redis.util.InternalRedisAPI
import org.redisson.misc.RedisURI

@InternalRedisAPI
data class RedissonConnectionKey(
    val ssl: Boolean,
    val unixDomainSocket: Boolean,
    val host: String,
    val port: Int,
    val username: String?,
    val password: String?,
) {
    override fun toString(): String = buildString {
        append(
            when {
                unixDomainSocket -> "redis+uds://"
                ssl -> "rediss://"
                else -> "redis://"
            }
        )
        if (username != null || password != null) {
            append(username ?: "")
            if (password != null) append(":***")
            append('@')
        }
        append(host)
        if (!unixDomainSocket) append(':').append(port)
    }

    @InternalRedisAPI
    companion object {
        fun of(redisURI: RedisURI): RedissonConnectionKey = RedissonConnectionKey(
            ssl = redisURI.isSsl,
            unixDomainSocket = redisURI.isUDS,
            host = redisURI.host,
            port = redisURI.port,
            username = redisURI.username,
            password = redisURI.password,
        )
    }
}
