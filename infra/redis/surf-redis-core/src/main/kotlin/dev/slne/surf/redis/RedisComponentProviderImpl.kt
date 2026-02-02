package dev.slne.surf.redis

import com.google.auto.service.AutoService
import dev.slne.surf.redis.cache.*
import dev.slne.surf.redis.config.RedisConfig
import dev.slne.surf.redis.event.RedisEventBus
import dev.slne.surf.redis.event.RedisEventBusImpl
import dev.slne.surf.redis.internal.RedissonConfigDetails
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
import io.netty.channel.epoll.Epoll
import kotlinx.serialization.KSerializer
import org.redisson.config.Config
import org.redisson.config.EqualJitterDelay
import org.redisson.config.Protocol
import org.redisson.config.TransportMode
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@AutoService(RedisComponentProvider::class)
class RedisComponentProviderImpl : RedisComponentProvider {
    private val transportMode = if (Epoll.isAvailable()) {
        TransportMode.EPOLL
    } else {
        TransportMode.NIO
    }

    override val eventLoopGroup get() = RedisInstance.instance.eventLoopGroup
    override val redissonExecutorService get() = RedisInstance.instance.redissonExecutorService
    override val clientId = UUID.randomUUID().toString()
        .split("-")
        .take(2)
        .joinToString("")

    override fun createRedissonConfig(details: RedissonConfigDetails): Config {
        val config = Config()
            .setPassword(details.redisURI.password)
            .setExecutor(redissonExecutorService)
            .setTransportMode(transportMode)
            .setEventLoopGroup(eventLoopGroup)
            .setTcpKeepAlive(true)
            .setTcpUserTimeout(10.seconds.inWholeMilliseconds.toInt())
            .setTcpKeepAliveCount(3)
            .setTcpKeepAliveInterval(10)
            .setTcpKeepAliveIdle(60)
            .setTcpNoDelay(true)
            .setKeepPubSubOrder(false)
            .setProtocol(Protocol.RESP3)
            .apply {
                useSingleServer()
                    .setConnectionMinimumIdleSize(4)
                    .setConnectionPoolSize(16)
                    .setClientName(RedisConfig.getConfig().clientName + "-" + details.pluginName)
                    .setPingConnectionInterval(10.seconds.inWholeMilliseconds.toInt())
                    .setConnectTimeout(5.seconds.inWholeMilliseconds.toInt())
                    .setRetryAttempts(10)
                    .setRetryDelay(EqualJitterDelay(200.milliseconds.toJavaDuration(), 1.seconds.toJavaDuration()))
                    .setAddress(details.redisURI.toString())
            }

        return config
    }

    override fun tryExtractPluginNameFromClass(clazz: Class<*>): String {
        return RedisInstance.get().tryExtractPluginNameFromClass(clazz)
    }

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