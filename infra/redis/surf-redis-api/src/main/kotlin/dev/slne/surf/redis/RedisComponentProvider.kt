package dev.slne.surf.redis

import dev.slne.surf.redis.cache.RedisSetIndexes
import dev.slne.surf.redis.cache.SimpleRedisCache
import dev.slne.surf.redis.cache.SimpleSetRedisCache
import dev.slne.surf.redis.event.RedisEvent
import dev.slne.surf.redis.event.RedisEventBus
import dev.slne.surf.redis.request.RedisRequest
import dev.slne.surf.redis.request.RequestResponseBus
import dev.slne.surf.redis.sync.list.SyncList
import dev.slne.surf.redis.sync.map.SyncMap
import dev.slne.surf.redis.sync.set.SyncSet
import dev.slne.surf.redis.sync.value.SyncValue
import dev.slne.surf.redis.util.InternalRedisAPI
import dev.slne.surf.surfapi.core.api.util.requiredService
import io.netty.channel.MultiThreadIoEventLoopGroup
import kotlinx.serialization.KSerializer
import java.util.concurrent.ExecutorService
import kotlin.time.Duration

@InternalRedisAPI
interface RedisComponentProvider {

    val eventLoopGroup: MultiThreadIoEventLoopGroup
    val redissonExecutorService: ExecutorService

    val clientId: String

    fun <K : Any, V : Any> createSimpleCache(
        namespace: String,
        serializer: KSerializer<V>,
        ttl: Duration,
        keyToString: (K) -> String,
        redisApi: RedisApi
    ): SimpleRedisCache<K, V>

    fun <T : Any> createSimpleSetRedisCache(
        namespace: String,
        serializer: KSerializer<T>,
        ttl: Duration,
        idOf: (T) -> String,
        indexes: RedisSetIndexes<T>,
        redisApi: RedisApi
    ): SimpleSetRedisCache<T>

    fun createEventBus(redisApi: RedisApi): RedisEventBus
    fun createRequestResponseBus(redisApi: RedisApi): RequestResponseBus

    fun <E : Any> createSyncList(
        id: String,
        elementSerializer: KSerializer<E>,
        ttl: Duration,
        api: RedisApi
    ): SyncList<E>

    fun <E : Any> createSyncSet(
        id: String,
        elementSerializer: KSerializer<E>,
        ttl: Duration,
        api: RedisApi
    ): SyncSet<E>

    fun <T : Any> createSyncValue(
        id: String,
        serializer: KSerializer<T>,
        defaultValue: T,
        ttl: Duration,
        api: RedisApi
    ): SyncValue<T>

    fun <K : Any, V : Any> createSyncMap(
        id: String,
        keySerializer: KSerializer<K>,
        valueSerializer: KSerializer<V>,
        ttl: Duration,
        api: RedisApi
    ): SyncMap<K, V>

    fun injectOriginId(event: RedisEvent) {
        event.originId = clientId
    }

    fun injectOriginId(request: RedisRequest) {
        request.originId = clientId
    }

    companion object {
        val instance = requiredService<RedisComponentProvider>()
        fun get() = instance
    }
}