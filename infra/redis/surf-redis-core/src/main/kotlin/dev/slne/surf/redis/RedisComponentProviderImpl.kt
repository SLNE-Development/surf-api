package dev.slne.surf.redis

import com.google.auto.service.AutoService
import dev.slne.surf.redis.cache.SimpleRedisCache
import dev.slne.surf.redis.cache.SimpleRedisCacheImpl
import dev.slne.surf.redis.event.RedisEventBus
import dev.slne.surf.redis.event.RedisEventBusImpl
import dev.slne.surf.redis.request.RequestResponseBus
import dev.slne.surf.redis.request.RequestResponseBusImpl
import kotlinx.serialization.KSerializer
import kotlin.time.Duration

@AutoService(RedisComponentProvider::class)
class RedisComponentProviderImpl : RedisComponentProvider {
    override val eventLoopGroup get() = RedisInstance.instance.eventLoopGroup

    override fun <K : Any, V : Any> createSimpleCache(
        namespace: String,
        serializer: KSerializer<V>,
        ttl: Duration,
        keyToString: (K) -> String,
        redisApi: RedisApi
    ): SimpleRedisCache<K, V> {
        return SimpleRedisCacheImpl(namespace, serializer, keyToString, ttl, redisApi)
    }

    override fun createEventBus(redisApi: RedisApi): RedisEventBus {
        return RedisEventBusImpl(redisApi)
    }

    override fun createRequestResponseBus(redisApi: RedisApi): RequestResponseBus {
        return RequestResponseBusImpl(redisApi)
    }
}