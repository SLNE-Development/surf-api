package dev.slne.surf.redis

import com.google.auto.service.AutoService
import dev.slne.surf.redis.cache.RedisSetIndexes
import dev.slne.surf.redis.cache.SimpleRedisCache
import dev.slne.surf.redis.cache.SimpleRedisCacheImpl
import dev.slne.surf.redis.cache.SimpleSetRedisCache
import dev.slne.surf.redis.cache.SimpleSetRedisCacheImpl
import dev.slne.surf.redis.event.RedisEventBus
import dev.slne.surf.redis.event.RedisEventBusImpl
import dev.slne.surf.redis.request.RequestResponseBus
import dev.slne.surf.redis.request.RequestResponseBusImpl
import dev.slne.surf.redis.sync.list.SyncList
import dev.slne.surf.redis.sync.list.SyncListImpl
import dev.slne.surf.redis.sync.map.SyncMap
import dev.slne.surf.redis.sync.map.SyncMapImpl
import dev.slne.surf.redis.sync.set.SyncSet
import dev.slne.surf.redis.sync.set.SyncSetImpl
import dev.slne.surf.redis.sync.value.SyncValue
import dev.slne.surf.redis.sync.value.SyncValueImpl
import kotlinx.serialization.KSerializer
import java.util.*
import kotlin.time.Duration

@AutoService(RedisComponentProvider::class)
class RedisComponentProviderImpl : RedisComponentProvider {
    override val eventLoopGroup get() = RedisInstance.instance.eventLoopGroup
    override val redissonExecutorService get() = RedisInstance.instance.redissonExecutorService
    override val clientId = UUID.randomUUID().toString()
        .split("-")
        .take(2)
        .joinToString("")

    override fun <K : Any, V : Any> createSimpleCache(
        namespace: String,
        serializer: KSerializer<V>,
        ttl: Duration,
        keyToString: (K) -> String,
        redisApi: RedisApi
    ): SimpleRedisCache<K, V> {
        return SimpleRedisCacheImpl(namespace, serializer, keyToString, ttl, redisApi)
    }

    override fun <T : Any> createSimpleSetRedisCache(
        namespace: String,
        serializer: KSerializer<T>,
        ttl: Duration,
        idOf: (T) -> String,
        indexes: RedisSetIndexes<T>,
        redisApi: RedisApi
    ): SimpleSetRedisCache<T> {
        return SimpleSetRedisCacheImpl(namespace, serializer, idOf, indexes, ttl, redisApi)
    }

    override fun createEventBus(redisApi: RedisApi): RedisEventBus {
        return RedisEventBusImpl(redisApi)
    }

    override fun createRequestResponseBus(redisApi: RedisApi): RequestResponseBus {
        return RequestResponseBusImpl(redisApi)
    }

    override fun <E : Any> createSyncList(
        id: String,
        elementSerializer: KSerializer<E>,
        ttl: Duration,
        api: RedisApi
    ): SyncList<E> {
        return SyncListImpl(api, id, ttl, elementSerializer)
    }

    override fun <E : Any> createSyncSet(
        id: String,
        elementSerializer: KSerializer<E>,
        ttl: Duration,
        api: RedisApi
    ): SyncSet<E> {
        return SyncSetImpl(api, id, ttl, elementSerializer)
    }

    override fun <T : Any> createSyncValue(
        id: String,
        serializer: KSerializer<T>,
        defaultValue: T,
        ttl: Duration,
        api: RedisApi
    ): SyncValue<T> {
        return SyncValueImpl(api, id, serializer, defaultValue, ttl)
    }

    override fun <K : Any, V : Any> createSyncMap(
        id: String,
        keySerializer: KSerializer<K>,
        valueSerializer: KSerializer<V>,
        ttl: Duration,
        api: RedisApi
    ): SyncMap<K, V> {
        return SyncMapImpl(api, id, ttl, keySerializer, valueSerializer)
    }
}