package dev.slne.surf.redis

import dev.slne.surf.redis.cache.SimpleRedisCache
import dev.slne.surf.redis.event.RedisEventBus
import dev.slne.surf.redis.request.RequestResponseBus
import dev.slne.surf.redis.util.InternalRedisAPI
import dev.slne.surf.surfapi.core.api.util.requiredService
import io.netty.channel.MultiThreadIoEventLoopGroup
import kotlinx.serialization.KSerializer
import kotlin.time.Duration

@InternalRedisAPI
interface RedisComponentProvider {

    val eventLoopGroup: MultiThreadIoEventLoopGroup

    fun <K : Any, V : Any> createSimpleCache(
        namespace: String,
        serializer: KSerializer<V>,
        ttl: Duration,
        keyToString: (K) -> String,
        redisApi: RedisApi
    ): SimpleRedisCache<K, V>

    fun createEventBus(redisApi: RedisApi): RedisEventBus
    fun createRequestResponseBus(redisApi: RedisApi): RequestResponseBus

    companion object {
        val instance = requiredService<RedisComponentProvider>()
        fun get() = instance
    }
}