package dev.slne.surf.api.redis

import dev.slne.surf.redis.RedisComponentProvider
import dev.slne.surf.redis.RedisComponentProviderImpl
import dev.slne.surf.redis.RedisInstance
import dev.slne.surf.redis.credentials.RedisCredentialsProvider
import dev.slne.surf.redis.credentials.RedisCredentialsProviderImpl
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy

internal const val REDIS_INSTANCE_BEAN = "surfRedisInstance"

/**
 * Publishes the process-wide Redis components as beans of surf-api's root context, where the
 * static accessors `RedisInstance.instance`, `RedisComponentProvider.instance` and
 * `RedisCredentialsProvider.instance` read them.
 *
 * The [RedisInstance] is lazy: creating it starts the Redis networking, so it exists only once a
 * plugin uses Redis. [RedisNetworking] stops it.
 */
@Configuration(proxyBeanMethods = false)
internal class RedisBeans {

    @Bean
    fun redisComponentProvider(): RedisComponentProvider = RedisComponentProviderImpl()

    @Bean
    fun redisCredentialsProvider(): RedisCredentialsProvider = RedisCredentialsProviderImpl()

    @Bean(REDIS_INSTANCE_BEAN)
    @Lazy
    fun redisInstance(): RedisInstance = BundledRedisInstance()
}
